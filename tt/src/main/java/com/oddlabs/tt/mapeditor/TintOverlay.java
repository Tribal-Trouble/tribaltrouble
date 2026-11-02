package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.PatchMesh;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tints the ground with the chosen {@link Overlay}, one colour per height map cell blending between cells, so a
 * region's border shades into the cliff beside it.
 *
 * <p>The island is measured and the tint painted on a worker thread, from the island as it was when the work
 * began, so measuring a large island does not stall the editor; the tint catches up a moment after an edit.
 *
 * <p>It draws the landscape's own patch mesh again over the patches the landscape drew this frame, placed from the
 * height texture exactly as {@code LandscapeShader} places it, and blends the tint over the ground.
 */
final class TintOverlay implements AutoCloseable {
    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec2 in_Position;
            layout(location = 4) in vec2 in_InstancePatchOffset;

            uniform float u_WorldSize;
            uniform sampler2D u_HeightMap;

            out vec2 v_uv;
            out float v_fogDist;

            void main() {
                // As LandscapeShader: one height texel per grid unit, sampled at its centre.
                vec2 worldPos = in_InstancePatchOffset + in_Position;
                vec2 uv = (worldPos + 1.0) / u_WorldSize;
                float h = texture(u_HeightMap, uv).r;
                vec4 viewPosition = u_viewMatrix * vec4(worldPos, h, 1.0);
                gl_Position = u_projectionMatrix * viewPosition;
                v_uv = uv;
                v_fogDist = length(viewPosition.xyz);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + FogShader.FOG_FUNCTION + """
            uniform sampler2D u_Overlay;

            in vec2 v_uv;
            in float v_fogDist;

            layout(location = 0) out vec4 out_FragColor;

            void main() {
                // Premultiplied, so fading into the fog fades all of it.
                out_FragColor = texture(u_Overlay, v_uv) * calculateFogFactor(v_fogDist, gl_FragCoord.xy);
            }
            """;

    private static final class OverlayShader extends ShaderProgram {
        OverlayShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            link();
        }
    }

    private final float @NonNull [] @NonNull [] heights;
    private final @NonNull AccessMap access;
    private final float height_scale;
    private MapAnalysis.@NonNull Resources resources = (_, _) -> null;
    private final int size;
    private final @NonNull OverlayShader shader = new OverlayShader();
    private final @NonNull PatchMesh patch_mesh = new PatchMesh();
    private final @NonNull Texture overlay;
    // Painted by the worker, then uploaded here; only one painting is under way at a time.
    private final @NonNull ByteBuffer upload;
    private @NonNull FloatVBO instances = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 2);
    private @NonNull FloatBuffer instance_buffer = BufferUtils.createFloatBuffer(1024 * 2);
    private final @NonNull ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Map editor overlay");
        thread.setDaemon(true);
        return thread;
    });

    private @Nullable Overlay shown;
    // Bumped by every change to the island.
    private int version;
    // The painting under way: the overlay and the version it paints.
    private @Nullable Future<?> painting;
    private @Nullable Overlay painting_overlay;
    private int painting_version;
    // The version the texture holds, or -1 when it holds nothing worth showing.
    private int painted = -1;
    private boolean closed;

    /**
     * @param heights the height map in meters, as the terrain editor keeps it
     */
    TintOverlay(float @NonNull [] @NonNull [] heights, @NonNull AccessMap access, @NonNull MapSettings settings) {
        this.heights = heights;
        this.access = access;
        this.height_scale = settings.getHeightScale();
        this.size = access.getSize();
        this.overlay = new Texture(size, size, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
        this.upload = BufferUtils.createByteBuffer(size * size * 4);
    }

    /** Gives the resources to measure; the layer is built after this, as it reports its changes here. */
    void setResources(MapAnalysis.@NonNull Resources resources) {
        this.resources = resources;
        mapChanged();
    }

    /** Notes that the island changed: the playable area was sorted again, or resources came or went. */
    void mapChanged() {
        version++;
    }

    /** A colour as the four bytes of an RGBA texel, premultiplied, read as one int in a buffer's native byte order. */
    static int texel(float red, float green, float blue, float opacity) {
        ByteBuffer bytes = BufferUtils.createByteBuffer(4);
        bytes.put((byte) Math.round(255 * red * opacity));
        bytes.put((byte) Math.round(255 * green * opacity));
        bytes.put((byte) Math.round(255 * blue * opacity));
        bytes.put((byte) Math.round(255 * opacity));
        return bytes.flip().getInt();
    }

    /** Shows an overlay's tint, or none. */
    void show(@Nullable Overlay chosen) {
        Overlay tint = chosen != null && chosen.isTint() ? chosen : null;
        if (tint != shown)
            painted = -1;
        shown = tint;
    }

    /**
     * Uploads a finished painting, and starts painting again if the island changed since the last one began.
     *
     * @param now whether to start now; while a stroke goes on it is only now and then, as capturing the island
     *            takes a moment on a large one
     */
    void update(@NonNull RenderContext context, boolean now) {
        if (closed)
            return;
        Future<?> under_way = painting;
        if (under_way != null) {
            if (!under_way.isDone())
                return;
            painting = null;
            try {
                under_way.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                throw new IllegalStateException("Could not measure the island", e.getCause());
            }
            // An overlay chosen since is painted afresh.
            if (painting_overlay == shown) {
                upload(context);
                painted = painting_version;
            }
        }
        Overlay current = shown;
        if (current == null || painted == version || (painted != -1 && !now))
            return;
        MapAnalysis analysis = MapAnalysis.capture(heights, access, resources, height_scale);
        painting_overlay = current;
        painting_version = version;
        painting = worker.submit(() -> current.paint(analysis, upload.clear().asIntBuffer()));
    }

    private void upload(@NonNull RenderContext context) {
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        context.setTexture(0, overlay);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                upload.clear());
    }

    /** Draws the tint over the ground the landscape just drew, from a delegate's render3D. */
    void render(@NonNull RenderContext context, @NonNull LandscapeRenderer landscape, @NonNull CameraState state) {
        if (shown == null || painted == -1 || closed)
            return;
        // The water reflection is drawn from a camera mirrored below the sea; the tint has no place in it.
        if (state.getCurrentZ() < landscape.getHeightMap().getSeaLevelMeters())
            return;
        List<LandscapeLeaf> patches = landscape.getVisiblePatches();
        if (patches.isEmpty())
            return;
        fillInstances(patches, landscape.getHeightMap().getMetersPerPatch());

        try (ScopedState _ = shader.use(); ScopedState _ = context.withBlendMode(
                BlendMode.PREMULTIPLIED); ScopedState _ = context.withDepthMode(
                        DepthMode.READ_ONLY); ScopedState _ = context.withCullMode(CullMode.NONE)) {
            shader.setUniform("u_WorldSize", (float) landscape.getHeightMap().getMetersPerWorld());
            context.setTexture(0, overlay);
            shader.setUniform("u_Overlay", 0);
            context.setTexture(1, landscape.getHeightMap().getHeightTexture());
            shader.setUniform("u_HeightMap", 1);
            // Pulled towards the camera to win against the ground it lies on, as decals are.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-16f, -32f);
            // The mask buffer is not ours to write.
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

            // Patch offsets per instance, set up the way LandscapeRenderer does it.
            patch_mesh.bind();
            instances.makeCurrent();
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instance_buffer);
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 2, GL11.GL_FLOAT, false, 0, 0);
            GL33.glVertexAttribDivisor(4, 1);
            patch_mesh.drawInstanced(patches.size());
            GL33.glVertexAttribDivisor(4, 0);
            GL20.glDisableVertexAttribArray(4);
            patch_mesh.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffers = stack.mallocInt(2);
                buffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
                GL20.glDrawBuffers(buffers);
            }
        }
    }

    private void fillInstances(@NonNull List<LandscapeLeaf> patches, float patch_size) {
        int floats = patches.size() * 2;
        if (instance_buffer.capacity() < floats) {
            int capacity = Math.max(instance_buffer.capacity() * 2, floats);
            instance_buffer = BufferUtils.createFloatBuffer(capacity);
            instances.close();
            instances = new FloatVBO(GL15.GL_STREAM_DRAW, capacity);
        }
        instance_buffer.clear();
        for (LandscapeLeaf patch : patches) {
            instance_buffer.put(patch.getPatchX() * patch_size);
            instance_buffer.put(patch.getPatchY() * patch_size);
        }
        instance_buffer.flip();
    }

    @Override
    public void close() {
        // The editor keeps drawing while the screen fades out, so later calls must do nothing.
        closed = true;
        worker.shutdownNow();
        shader.close();
        patch_mesh.delete();
        overlay.close();
        instances.close();
    }
}
