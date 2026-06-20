package com.oddlabs.tt.scenery;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.procedural.GeneratorClouds;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SceneRenderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.SeaBottomShader;
import com.oddlabs.tt.render.shader.SkyShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.util.Stitcher;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Renders the sky dome, clouds, and background water scenery (sea bottom and outer water).
 */
public final class Sky implements SceneRenderer, AutoCloseable {
    private static final float[] SKYDOME_SPEED_OUTER = {0.2f, 0f};
    private static final float[] SKYDOME_SPEED_INNER = {0.4f, 0f};
    private static final float SKYDOME_HEIGHT = 0f;
    private static final int SKYDOME_GRADIENT_LENGTH = 20;
    private static final int SKYDOME_DEFAULT_COLOR = 8;
    private static final int FLOATS_PER_VERTEX = 13;

    private static final Map<Terrain, @NonNull Color> SKYDOME_INITCOLOR = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_E5_F2_FF),
            Terrain.VIKING, new Color.Standard(0xFF_FF_E5_A5)
    ));

    private static final Map<Terrain, Color.@NonNull Linear> SKYDOME_INTENSITY = new EnumMap<>(Map.of(
            Terrain.NATIVE, (Color.Linear) Color.Linear.WHITE,
            Terrain.VIKING, new Color.Linear(1.5f, 1f, 1f, 1f)
    ));

    private static final Map<Terrain, Color.@NonNull Standard> SKYDOME_GRADIENT = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_BF_D2_F2),
            Terrain.VIKING, new Color.Standard(0xFF_99_99_D8)
    ));

    private static final Map<Terrain, Color.@NonNull Linear> TEX_ENV_COLOR = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_F2_F8_FF).linear(),
            Terrain.VIKING, new Color.Standard(0xFF_FF_F2_CC).linear()
    ));

    public static final Map<Terrain, Color.@NonNull Linear> SEA_BOTTOM_COLOR = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_73_40_99).linear(),
            Terrain.VIKING, Color.Linear.BLACK
    ));

    private static final float SKYDOME_OUTER_UTILING = 8f;
    private static final float SKYDOME_OUTER_VTILING = 8f;
    private static final float SKYDOME_INNER_UTILING = 8f;
    private static final float SKYDOME_INNER_VTILING = 8f;

    private static final int NUM_WATER_RINGS = 6;

    private static final float START_ANGLE = -(float) Math.PI / 4f;

    private final Color.@NonNull Linear skyColor;
    private final Color.@NonNull Linear seaBottomColor;
    private final ShortVBO @NonNull [] strip_indices;
    private final @NonNull ShortVBO fan_indices;
    private final @NonNull FloatVBO water_vertices;
    private final @NonNull FloatVBO bottom_vertices;
    private final @NonNull ShortVBO water_indices;
    private final @NonNull FloatVBO sky_vbo;

    private final @NonNull Texture @NonNull [] clouds;
    private final int subdiv_axis;
    private final int subdiv_height;
    private final @NonNull Terrain terrain;

    private final SkyShader skyShader = new SkyShader();
    private final SeaBottomShader seaBottomShader = new SeaBottomShader();
    private final @NonNull Texture detail;
    private final @NonNull Texture detailNormal;
    private final @NonNull VertexArray skyVAO;
    private final @NonNull VertexArray seaBottomVAO;

    // Cloud animation state
    private final float[] innerOffset = new float[2];
    private final float[] outerOffset = new float[2];

    // Inner layer state
    private float innerDirection = 0f;
    private float innerSpeed = SKYDOME_SPEED_INNER[0] * 0.01f;
    private float targetInnerDirection = innerDirection;
    private float targetInnerSpeed = innerSpeed;
    private float innerTimeSinceChange = 0f;
    private float innerChangeInterval = 20f;

    // Outer layer state
    private float outerDirection = 0f;
    private float outerSpeed = SKYDOME_SPEED_OUTER[0] * 0.01f;
    private float targetOuterDirection = outerDirection;
    private float targetOuterSpeed = outerSpeed;
    private float outerTimeSinceChange = 0f;
    private float outerChangeInterval = 25f;

    // Cloud density state
    private float innerCloudDensity = 0f;
    private float targetInnerCloudDensity = 0f;
    private float outerCloudDensity = 0f;
    private float targetOuterCloudDensity = 0f;
    private float densityTimeSinceChange = 0f;
    private float densityChangeInterval = 60f;

    private float lastTime = 0f;

    public Sky(@NonNull LandscapeRenderer renderer, @NonNull Terrain terrain, @NonNull Texture detail,
            @NonNull Texture detailNormal) {
        this(renderer, terrain, (float) (renderer.getHeightMap().getMetersPerWorld() * Math.sqrt(2) / 2), 6000f, 20, 20,
                SKYDOME_OUTER_UTILING, SKYDOME_OUTER_VTILING, SKYDOME_INNER_UTILING, SKYDOME_INNER_VTILING, renderer
                        .getHeightMap().getMetersPerWorld() / 2f, renderer.getHeightMap().getMetersPerWorld() / 2f,
                SKYDOME_HEIGHT, detail, detailNormal);
    }

    private Sky(@NonNull LandscapeRenderer landscape_renderer, @NonNull Terrain terrain,
            float inner_radius, float radius, int subdiv_axis, int subdiv_height, float outer_utile, float outer_vtile,
            float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z,
            @NonNull Texture detail, @NonNull Texture detailNormal) {
        this.terrain = terrain;
        this.detail = detail;
        this.detailNormal = detailNormal;
        this.subdiv_axis = subdiv_axis;
        this.subdiv_height = subdiv_height;
        this.skyColor = TEX_ENV_COLOR.get(terrain);
        this.seaBottomColor = SEA_BOTTOM_COLOR.get(terrain);
        TextureGenerator clouds_desc = new GeneratorClouds(terrain);
        clouds = Resources.findResource(clouds_desc);

        // Create interleaved VBO for the sky
        int num_vertices_sky = subdiv_axis * (subdiv_height - 1) + 1;
        int stride = FLOATS_PER_VERTEX * Float.BYTES; // pos, norm, uv0, uv1, color
        try (var stack = MemoryStack.stackPush()) {
            FloatBuffer skyBuffer = stack.mallocFloat(num_vertices_sky * (stride / Float.BYTES));
            makeSkyVertices(radius, outer_utile, outer_vtile, inner_utile, inner_vtile, origin_x, origin_y, origin_z,
                    skyBuffer);
            skyBuffer.flip();
            sky_vbo = new FloatVBO(GL15.GL_STATIC_DRAW, skyBuffer);
        }

        strip_indices = makeSkyStripIndices();
        fan_indices = makeSkyFanIndices();

        // --- Sea bottom and water stitching logic ---
        List<SkyStitchVertex[]> vertices_stitch_list = new ArrayList<>();
        List<ShortBuffer> stitch_indices_list = new ArrayList<>();
        int num_vertices_water = 0;
        int num_indices = 0;
        SkyStitchVertex[] previous_vertices = makeLandscapeVertices(landscape_renderer.getHeightMap());
        vertices_stitch_list.add(previous_vertices);
        num_vertices_water += previous_vertices.length;
        for (int i = 0; i < NUM_WATER_RINGS; i++) {
            float radius_factor = (float) (i + 1) / NUM_WATER_RINGS;
            float ring_radius = inner_radius + (float) Math.pow(radius - inner_radius, radius_factor);
            SkyStitchVertex[] ring_vertices = makeDomeVertices(landscape_renderer.getHeightMap(), i + 1,
                    num_vertices_water, ring_radius, origin_x, origin_y);
            vertices_stitch_list.add(ring_vertices);
            num_vertices_water += ring_vertices.length;
            SkyStitchVertex[] stitch_vertices = new SkyStitchVertex[ring_vertices.length + previous_vertices.length];
            System.arraycopy(previous_vertices, 0, stitch_vertices, 0, previous_vertices.length);
            System.arraycopy(ring_vertices, 0, stitch_vertices, previous_vertices.length, ring_vertices.length);
            ShortBuffer stitch_indices = Stitcher.stitch(stitch_vertices);
            stitch_indices_list.add(stitch_indices);
            num_indices += stitch_indices.remaining();
            previous_vertices = ring_vertices;
        }
        SkyStitchVertex[] all_vertices = new SkyStitchVertex[num_vertices_water];
        int index = 0;
        for (SkyStitchVertex[] vertices : vertices_stitch_list) {
            System.arraycopy(vertices, 0, all_vertices, index, vertices.length);
            index += vertices.length;
        }
        ShortBuffer all_indices = BufferUtils.createShortBuffer(num_indices);
        for (ShortBuffer indices : stitch_indices_list) {
            all_indices.put(indices);
        }
        all_indices.flip();
        water_indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices);
        water_vertices = toVBO(all_vertices, landscape_renderer.getHeightMap().getSeaLevelMeters());
        bottom_vertices = toBottomVBO(all_vertices, landscape_renderer.getHeightMap());

        this.skyVAO = new VertexArray();
        skyVAO.bind();
        sky_vbo.bind();
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1); // Normal
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2); // TexCoord0
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, 6 * Float.BYTES);
        GL20.glEnableVertexAttribArray(4); // TexCoord1
        GL20.glVertexAttribPointer(4, 2, GL11.GL_FLOAT, false, stride, 8 * Float.BYTES);
        GL20.glEnableVertexAttribArray(3); // Color
        GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 10 * Float.BYTES);
        skyVAO.unbind();

        this.seaBottomVAO = new VertexArray();
        seaBottomVAO.bind();
        bottom_vertices.bind();
        GL20.glEnableVertexAttribArray(0); // Position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        seaBottomVAO.unbind();
    }

    @Override
    public void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelView,
            @NonNull MatrixStack projection) {
        try (var _ = skyShader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.BACK)) {

            skyShader.setUniform(SkyShader.Uniforms.MODEL_VIEW_MATRIX, modelView.current());
            skyShader.setUniform(SkyShader.Uniforms.SKY_COLOR, skyColor);

            context.setTexture(0, clouds[GeneratorClouds.INNER]);
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_0, 0);

            context.setTexture(1, clouds[GeneratorClouds.OUTER]);
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_1, 1);

            updateAnimation();

            skyShader.setUniform(SkyShader.Uniforms.INNER_OFFSET, innerOffset[0], innerOffset[1]);
            skyShader.setUniform(SkyShader.Uniforms.OUTER_OFFSET, outerOffset[0], outerOffset[1]);

            skyVAO.bind();

            for (ShortVBO strip_index : strip_indices) {
                strip_index.drawElements(GL11.GL_TRIANGLE_STRIP, subdiv_axis * 2 + 2, 0);
            }
            fan_indices.drawElements(GL11.GL_TRIANGLE_FAN, subdiv_axis + 2, 0);

            skyVAO.unbind();
        } finally {
            VBO.releaseIndexVBO(context);
        }
    }

    private void updateAnimation() {
        float currentTime = Renderer.getRenderer().getEventQueue().getTime();
        float dt = currentTime - lastTime;
        if (dt < 0 || dt > 1.0f) dt = 0.016f;
        lastTime = currentTime;

        var random = ThreadLocalRandom.current();
        innerTimeSinceChange += dt;
        if (innerTimeSinceChange > innerChangeInterval) {
            innerTimeSinceChange = 0f;
            innerChangeInterval = 30f + (float) random.nextGaussian() * 10f;
            float dirChange = (float) random.nextGaussian() * 10f;
            targetInnerDirection += (float) Math.toRadians(dirChange);
            float speedChange = innerSpeed * (float) random.nextGaussian() * 0.1f;
            targetInnerSpeed = Math.clamp(targetInnerSpeed + speedChange, 0.002f, 0.008f);
        }
        innerDirection += (targetInnerDirection - innerDirection) * dt * 0.2f;
        innerSpeed += (targetInnerSpeed - innerSpeed) * dt * 0.2f;

        innerOffset[0] += (float) Math.cos(innerDirection) * innerSpeed * dt;
        innerOffset[1] += (float) Math.sin(innerDirection) * innerSpeed * dt;

        outerTimeSinceChange += dt;
        if (outerTimeSinceChange > outerChangeInterval) {
            outerTimeSinceChange = 0f;
            outerChangeInterval = 40f + (float) random.nextGaussian() * 15f;
            float dirChange = (float) random.nextGaussian() * 8f;
            targetOuterDirection += (float) Math.toRadians(dirChange);
            float speedChange = outerSpeed * (float) random.nextGaussian() * 0.1f;
            targetOuterSpeed = Math.clamp(targetOuterSpeed + speedChange, 0.001f, 0.004f);
        }
        outerDirection += (targetOuterDirection - outerDirection) * dt * 0.1f;
        outerSpeed += (targetOuterSpeed - outerSpeed) * dt * 0.1f;

        outerOffset[0] += (float) Math.cos(outerDirection) * outerSpeed * dt;
        outerOffset[1] += (float) Math.sin(outerDirection) * outerSpeed * dt;

        densityTimeSinceChange += dt;
        if (densityTimeSinceChange > densityChangeInterval) {
            densityTimeSinceChange = 0f;
            densityChangeInterval = random.nextFloat(60f, 120f);
            float innerChange = (float) random.nextGaussian() * 0.1f;
            targetInnerCloudDensity = Math.clamp(innerChange, -0.2f, 0.2f);
            float outerChange = (float) random.nextGaussian() * 0.1f;
            targetOuterCloudDensity = Math.clamp(outerChange, -0.2f, 0.2f);
        }
        innerCloudDensity += (targetInnerCloudDensity - innerCloudDensity) * dt * 0.05f;
        outerCloudDensity += (targetOuterCloudDensity - outerCloudDensity) * dt * 0.05f;
    }

    public void renderSeaBottom(@NonNull RenderContext context, @NonNull CameraState state,
            @NonNull MatrixStack modelView, @NonNull MatrixStack projection) {
        try (var _ = seaBottomShader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context
                .withDepthMode(DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.BACK)) {

            seaBottomShader.setUniform(SeaBottomShader.Uniforms.MODEL_VIEW_MATRIX, modelView.current());

            seaBottomShader.setUniform(SeaBottomShader.Uniforms.BASE_COLOR, seaBottomColor);

            if (Globals.draw_detail) {
                context.setTexture(1, detail);
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.TEXTURE_1, 1);
                context.setTexture(2, detailNormal);
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.TEXTURE_NORMAL, 2);
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);
            } else {
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, 0f);
            }

            seaBottomVAO.bind();
            water_indices.drawElements(GL11.GL_TRIANGLES, water_indices.capacity(), 0);
            seaBottomVAO.unbind();

            context.setActiveTexture(0);
        } finally {
            VBO.releaseIndexVBO(context);
        }
    }

    private static @NonNull FloatVBO toVBO(SkyStitchVertex @NonNull [] vertices, float height) {
        FloatBuffer vertex_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(vertices.length * 3));
        for (SkyStitchVertex vertex : vertices) {
            float x = vertex.x;
            float y = vertex.y;
            float z = (height * (NUM_WATER_RINGS - vertex.getSide())) / NUM_WATER_RINGS;
            vertex_buffer.put(x).put(y).put(z);
        }
        vertex_buffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertex_buffer);
    }

    private static @NonNull FloatVBO toBottomVBO(SkyStitchVertex @NonNull [] vertices, @NonNull HeightMap heightmap) {
        float metersPerWorld = heightmap.getMetersPerWorld();
        float cx = metersPerWorld * 0.5f;
        float cy = metersPerWorld * 0.5f;
        FloatBuffer vertex_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(vertices.length * 3));
        for (SkyStitchVertex vertex : vertices) {
            float x = vertex.x;
            float y = vertex.y;
            float dx = x - cx;
            float dy = y - cy;
            float maxDist = Math.max(Math.abs(dx), Math.abs(dy));
            float bx;
            float by;
            if (maxDist < 1e-5f) {
                bx = cx;
                by = cy;
            } else {
                float t = cx / maxDist;
                bx = Math.clamp(cx + t * dx, 0f, metersPerWorld);
                by = Math.clamp(cy + t * dy, 0f, metersPerWorld);
            }
            float boundaryHeight = heightmap.getNearestHeight(bx, by);
            float z = (boundaryHeight * (NUM_WATER_RINGS - vertex.getSide())) / NUM_WATER_RINGS;

            if (vertex.getSide() == 0) {
                float dxCenter = cx - x;
                float dyCenter = cy - y;
                float distToCenter = (float) Math.sqrt(dxCenter * dxCenter + dyCenter * dyCenter);
                if (distToCenter > 1e-5f) {
                    float overlap = 1.0f;
                    x += (dxCenter / distToCenter) * overlap;
                    y += (dyCenter / distToCenter) * overlap;
                }
            }

            vertex_buffer.put(x).put(y).put(z);
        }
        vertex_buffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertex_buffer);
    }

    public @NonNull FloatVBO getWaterVertices() {
        return water_vertices;
    }

    public Color.@NonNull Linear getSkyColor() {
        return skyColor;
    }

    public @NonNull ShortVBO getWaterIndices() {
        return water_indices;
    }

    public float @NonNull [] getInnerOffset() {
        return innerOffset;
    }

    public float @NonNull [] getOuterOffset() {
        return outerOffset;
    }

    public float getInnerCloudDensity() {
        return innerCloudDensity;
    }

    public float getOuterCloudDensity() {
        return outerCloudDensity;
    }

    public @NonNull Texture @NonNull [] getClouds() {
        return clouds;
    }

    private void makeSkyVertices(float radius, float outer_utile, float outer_vtile, float inner_utile,
            float inner_vtile, float origin_x, float origin_y, float origin_z, @NonNull FloatBuffer buffer) {
        float r;
        float x, y, z;
        float height_coeff;
        float dome_height = radius;
        float h_angle_inc = ((float) Math.PI / 2) / (subdiv_height - 1);
        float a_angle_inc = (float) Math.PI * 2 / subdiv_axis;
        float offset_angle = a_angle_inc / 2f;

        // skydome_default_color is authored to be darker in the original game (sRGB space)
        Color.Standard skydome_gradient_const = SKYDOME_GRADIENT.get(terrain);
        Color.Linear skydome_default_linear = new Color.Standard(
                (float) Math.pow(skydome_gradient_const.r(), SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(skydome_gradient_const.g(), SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(skydome_gradient_const.b(), SKYDOME_DEFAULT_COLOR),
                1.0f
        ).linear();

        Color.Linear[] skydome_gradient = new Color.Linear[SKYDOME_GRADIENT_LENGTH];
        Color.Linear initialLinear = new Color.Linear(SKYDOME_INITCOLOR.get(terrain));
        skydome_gradient[0] = initialLinear;

        float alpha;
        Color.Linear prevLinear = initialLinear;
        Color.Linear skydome_gradient_const_linear = new Color.Linear(skydome_gradient_const);
        Color.Linear skydome_intensity = SKYDOME_INTENSITY.get(terrain);
        for (int i = 1; i < SKYDOME_GRADIENT_LENGTH; i++) {
            alpha = (float) i / (SKYDOME_GRADIENT_LENGTH - 1);

            // Interpolation and multiplication now happen in linear space
            Color.Linear currentLinear = new Color.Linear(
                    alpha * skydome_default_linear.r() + (1f - alpha) * prevLinear.r() * skydome_gradient_const_linear
                            .r(),
                    alpha * skydome_default_linear.g() + (1f - alpha) * prevLinear.g() * skydome_gradient_const_linear
                            .g(),
                    alpha * skydome_default_linear.b() + (1f - alpha) * prevLinear.b() * skydome_gradient_const_linear
                            .b(),
                    1.0f);

            skydome_gradient[i] = new Color.Linear(currentLinear).mul(skydome_intensity);
            prevLinear = currentLinear;
        }

        skydome_gradient[0] = new Color.Linear(initialLinear).mul(skydome_intensity);
        skydome_default_linear.mul(skydome_intensity);

        for (int i = 0; i < subdiv_height - 1; i++) {
            z = (float) Math.sin(h_angle_inc * i) * radius;
            r = (float) Math.cos(h_angle_inc * i) * radius;
            height_coeff = Math.abs(z) < 250f ? dome_height / 250f : dome_height / z;

            for (int j = 0; j < subdiv_axis; j++) {
                x = (float) Math.cos(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                y = (float) Math.sin(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;

                buffer.put(x + origin_x).put(y + origin_y).put(z + origin_z); // Position
                float inv_len = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);
                buffer.put(x * inv_len).put(y * inv_len).put(z * inv_len); // Normal
                buffer.put(x * height_coeff / (radius * outer_utile) + 0.5f).put(y * height_coeff / (radius
                        * outer_vtile) + 0.5f); // TexCoord0
                buffer.put(x * height_coeff / (radius * inner_utile) + 0.5f).put(y * height_coeff / (radius
                        * inner_vtile) + 0.5f); // TexCoord1
                Color colorVal = i < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[i] : skydome_default_linear;
                buffer.put(colorVal.r()).put(colorVal.g()).put(colorVal.b()); // Color
            }
        }
        buffer.put(origin_x).put(origin_y).put(radius + origin_z); // Position
        buffer.put(0).put(0).put(1); // Normal
        buffer.put(0.5f).put(0.5f); // TexCoord0
        buffer.put(0.5f).put(0.5f); // TexCoord1
        Color colorVal = subdiv_height - 1 < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[subdiv_height - 1]
                : skydome_default_linear;
        buffer.put(colorVal.r()).put(colorVal.g()).put(colorVal.b()); // Color
    }

    private @NonNull ShortVBO @NonNull [] makeSkyStripIndices() {
        ShortVBO[] strip_indices = new ShortVBO[subdiv_height - 2];
        try (var stack = MemoryStack.stackPush()) {
            for (int i = 0; i < strip_indices.length; i++) {
                int size = subdiv_axis * 2 + 2;
                ShortBuffer temp = stack.mallocShort(size);
                for (int j = 0; j < subdiv_axis; j++) {
                    temp.put(j * 2, (short) (i * subdiv_axis + j));
                    temp.put(j * 2 + 1, (short) ((i + 1) * subdiv_axis + j));
                }
                temp.put(subdiv_axis * 2, (short) (i * subdiv_axis));
                temp.put(subdiv_axis * 2 + 1, (short) ((i + 1) * subdiv_axis));
                strip_indices[i] = new ShortVBO(GL15.GL_STATIC_DRAW, size);
                temp.rewind();
                strip_indices[i].put(temp);
            }
        }
        return strip_indices;
    }

    private @NonNull ShortVBO makeSkyFanIndices() {
        int size = subdiv_axis + 2;
        try (var stack = MemoryStack.stackPush()) {
            ShortBuffer temp = stack.mallocShort(size);
            temp.put(0, (short) (sky_vbo.capacity() / FLOATS_PER_VERTEX - 1));
            for (int i = 0; i < subdiv_axis; i++) {
                temp.put(i + 1, (short) ((subdiv_height - 1) * subdiv_axis - i - 1));
            }
            temp.put(subdiv_axis + 1, (short) ((subdiv_height - 1) * subdiv_axis - 1));

            ShortVBO fan_indices = new ShortVBO(GL15.GL_STATIC_DRAW, size);
            temp.rewind();
            fan_indices.put(temp);
            return fan_indices;
        }
    }

    private @NonNull SkyStitchVertex @NonNull [] makeDomeVertices(@NonNull HeightMap heightmap, int ring_id,
            int index_offset, float radius, float origin_x, float origin_y) {
        float a_angle_inc = (float) Math.PI * 2 / subdiv_axis;
        return IntStream.range(0, subdiv_axis)
                .mapToObj(i -> {
                    int index = i + index_offset;
                    return new SkyStitchVertex(heightmap, index, ring_id,
                            (float) Math.cos(START_ANGLE + a_angle_inc * i) * radius + origin_x,
                            (float) Math.sin(START_ANGLE + a_angle_inc * i) * radius + origin_y);
                }).toArray(SkyStitchVertex[]::new);
    }

    private @NonNull SkyStitchVertex @NonNull [] makeLandscapeVertices(@NonNull HeightMap heightmap) {
        int gridUnitsPerWorld = heightmap.getGridUnitsPerWorld();
        int size = 4 * gridUnitsPerWorld;
        SkyStitchVertex[] result = new SkyStitchVertex[size];

        int metersPerUnit = HeightMap.METERS_PER_UNIT_GRID;
        int metersPerWorld = heightmap.getMetersPerWorld();

        for (int i = 0; i < gridUnitsPerWorld; i++) {
            int index = i;
            result[index] = new SkyStitchVertex(heightmap, index, 0, 0, i * metersPerUnit);

            index = i + gridUnitsPerWorld;
            result[index] = new SkyStitchVertex(heightmap, index, 0, i * metersPerUnit, metersPerWorld);

            index = i + gridUnitsPerWorld * 2;
            result[index] = new SkyStitchVertex(heightmap, index, 0, metersPerWorld, metersPerWorld - i
                    * metersPerUnit);

            index = i + gridUnitsPerWorld * 3;
            result[index] = new SkyStitchVertex(heightmap, index, 0, metersPerWorld - i * metersPerUnit, 0);
        }
        return result;
    }

    public static Landscape.@NonNull StructureLayers genSeabottom(
            Terrain terrain, int size,
            @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi4, @NonNull Channel voronoi8) {
        Color.Standard color = new Color.Standard(SEA_BOTTOM_COLOR.get(terrain));
        Layer bottom = new Layer(
                new Channel(size, size).fill(color.r()),
                new Channel(size, size).fill(color.g()),
                new Channel(size, size).fill(color.b()),
                new Channel(size, size).fill(0.2f) // Smooth (low detail)
        );
        return new Landscape.StructureLayers(bottom, getFlatNormal(size));
    }

    private static @NonNull Layer getFlatNormal(int size) {
        return new Layer(
                new Channel(size, size).fill(0.5f),
                new Channel(size, size).fill(0.5f),
                new Channel(size, size).fill(1.0f),
                new Channel(size, size).fill(0.0f) // Specular = 0
        );
    }

    private static class SkyStitchVertex extends Stitcher.Vertex<SkyStitchVertex> {
        private final float x;
        private final float y;
        private final float theta;
        private final @NonNull HeightMap heightmap;

        private SkyStitchVertex(@NonNull HeightMap heightmap, int index, int side, float x, float y) {
            super(index, side);
            this.heightmap = heightmap;
            this.x = x;
            this.y = y;
            float half_world_size = heightmap.getMetersPerWorld() * .5f;
            this.theta = (float) Math.atan2(y - half_world_size, x - half_world_size);
        }

        @Override
        public final int compareTo(@NonNull SkyStitchVertex o) {
            return -Float.compare(theta, o.theta);
        }
    }

    @Override
    public void close() {
        skyVAO.close();
        seaBottomVAO.close();
        skyShader.close();
        seaBottomShader.close();
        sky_vbo.close();
        water_vertices.close();
        bottom_vertices.close();
        water_indices.close();
        fan_indices.close();
        for (ShortVBO vbo : strip_indices) {
            vbo.close();
        }
    }
}
