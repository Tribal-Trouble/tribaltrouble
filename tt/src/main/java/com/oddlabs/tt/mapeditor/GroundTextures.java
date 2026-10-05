package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.FBO;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.vbo.QuadVBO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Keeps the island's ground texture in step with the edited heights.
 *
 * <p>The island's diffuse and normal textures are baked once by {@code LandscapeBaker}, which layers the ground
 * textures one pass at a time, each through its alpha map. Here the alphas of an edited area are worked out again
 * ({@link GroundAlphas}) and the same layering is redone for just that area, in one pass that stacks every layer,
 * straight into the island's textures.
 */
final class GroundTextures implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(GroundTextures.class.getName());

    /** The blend layers Landscape builds: seven ground textures with a lighting step after the fifth. */
    private static final int NUM_BLENDS = 8;
    private static final int LIGHTING_BLEND = 5;
    private static final int SHADOW_BLEND = 6;
    private static final int NUM_LAYERS = NUM_BLENDS - 1;

    private static final String VERTEX_SHADER = """
            #version 410 core
            layout(location = 0) in vec2 in_Position;
            void main() {
                gl_Position = vec4(in_Position, 0.0, 1.0);
            }
            """;

    /**
     * LandscapeBaker's blend steps, all in one pass. Each step is rounded to 8 bits, as the baker's passes store
     * their result in 8 bit textures between steps.
     */
    private static final String FRAGMENT_SHADER = """
            #version 410 core
            uniform sampler2D u_Layer[7];
            uniform sampler2D u_AlphaLow;  // alphas 0-3
            uniform sampler2D u_AlphaHigh; // alphas 4-6
            uniform float u_Size;
            uniform float u_TextureScale;
            uniform bool u_Diffuse;
            uniform vec3 u_LightColor;

            out vec4 out_Color;

            vec4 store(vec4 c) {
                return floor(clamp(c, 0.0, 1.0) * 255.0 + 0.5) / 255.0;
            }

            void main() {
                vec2 uv = gl_FragCoord.xy / u_Size;
                vec2 st = uv * u_TextureScale;
                vec4 low = texture(u_AlphaLow, uv);
                vec4 high = texture(u_AlphaHigh, uv);
                vec4 c = store(texture(u_Layer[0], st));
                c = store(mix(c, texture(u_Layer[1], st), low.r));
                c = store(mix(c, texture(u_Layer[2], st), low.g));
                c = store(mix(c, texture(u_Layer[3], st), low.b));
                c = store(mix(c, texture(u_Layer[4], st), low.a));
                if (u_Diffuse)
                    c = store(c + c * vec4(u_LightColor * high.r, 0.0));
                c = store(mix(c, texture(u_Layer[5], st), high.g));
                c = store(mix(c, texture(u_Layer[6], st), high.b));
                out_Color = c;
            }
            """;

    private static final class StackShader extends ShaderProgram {
        StackShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            link();
        }
    }

    // Texture bindings go through the render context so its binding cache stays true.
    private final @NonNull RenderContext context;
    private final @NonNull GroundAlphas alphas;
    private final int size;
    private final int colormap_size;
    private final int texels_per_cell;
    private final float texture_scale;
    private final @NonNull Texture diffuse;
    private final @NonNull Texture normal;
    private final @NonNull Texture @NonNull [] diffuse_layers = new Texture[NUM_LAYERS];
    private final @NonNull Texture @NonNull [] normal_layers = new Texture[NUM_LAYERS];
    private final float @NonNull [] light_color = new float[3];
    private final @NonNull Texture alpha_low;
    private final @NonNull Texture alpha_high;
    private final @NonNull ByteBuffer upload_low;
    private final @NonNull ByteBuffer upload_high;
    private final @NonNull FBO fbo;
    private final @NonNull StackShader shader = new StackShader();
    private final @NonNull QuadVBO quad = new QuadVBO();
    private final int mip_read_fbo = GL30.glGenFramebuffers();
    private final int mip_draw_fbo = GL30.glGenFramebuffers();

    private boolean closed;
    // Generated rocks and iron the generator left without a shadow, by cell.
    private final boolean @NonNull [] unshaded;

    // Heights changed since the last update (inclusive), in cells.
    private int dirty_x0 = Integer.MAX_VALUE;
    private int dirty_y0 = Integer.MAX_VALUE;
    private int dirty_x1 = Integer.MIN_VALUE;
    private int dirty_y1 = Integer.MIN_VALUE;

    /**
     * @param generated_supplies whether the trees, rocks and iron are the ones the generator placed, rather than
     *                           saved ones put in their place
     * @return the updater, or null when the island's blend layers are not laid out as expected, in which case the
     *         ground texture simply stays as generated
     */
    static @Nullable GroundTextures create(@NonNull WorldInfo world_info, @NonNull MapSettings settings,
            @NonNull RenderContext context, boolean generated_supplies) {
        BlendInfo[] blends = world_info.blend_infos();
        boolean expected = blends.length == NUM_BLENDS;
        for (int i = 0; expected && i < NUM_BLENDS; i++)
            expected = i == LIGHTING_BLEND ? blends[i] instanceof BlendLighting : blends[i] instanceof StructureBlend;
        if (!expected) {
            logger.warning("Unexpected ground blend layers; edited ground keeps its generated texture");
            return null;
        }
        return new GroundTextures(world_info, settings, context, generated_supplies);
    }

    private GroundTextures(@NonNull WorldInfo world_info, @NonNull MapSettings settings,
            @NonNull RenderContext context, boolean generated_supplies) {
        this.context = context;
        int grid_size = world_info.heightmap().length;
        this.unshaded = new boolean[grid_size * grid_size];
        List<int[]> shaded = new ArrayList<>();
        for (List<int[]> supplies : List.of(world_info.rocks(), world_info.iron())) {
            for (int[] supply : supplies) {
                if (!generated_supplies || castsShadow(world_info, supply))
                    shaded.add(supply);
                else
                    unshaded[supply[1] * grid_size + supply[0]] = true;
            }
        }
        this.alphas = new GroundAlphas(world_info.heightmap(), settings, world_info.trees(), world_info.palm_trees(),
                shaded);
        this.size = alphas.getSize();
        this.colormap_size = world_info.texels_per_colormap();
        this.texels_per_cell = colormap_size / size;
        // As IslandGenerator hands it to LandscapeBaker.
        this.texture_scale = (float) colormap_size / Globals.STRUCTURE_SIZE;
        this.diffuse = world_info.maps().diffuse();
        this.normal = world_info.maps().normal();
        BlendInfo[] blends = world_info.blend_infos();
        int layer = 0;
        for (BlendInfo blend : blends) {
            if (blend instanceof StructureBlend structure) {
                diffuse_layers[layer] = structure.getStructureMap();
                normal_layers[layer] = structure.getNormalMap();
                layer++;
            } else if (blend instanceof BlendLighting lighting) {
                // LandscapeBaker passes the colour as (r, b, g), reproducing an old quirk.
                light_color[0] = lighting.getR();
                light_color[1] = lighting.getB();
                light_color[2] = lighting.getG();
            }
        }
        this.alpha_low = newAlphaTexture();
        this.alpha_high = newAlphaTexture();
        this.upload_low = BufferUtils.createByteBuffer(size * size * 4);
        this.upload_high = BufferUtils.createByteBuffer(size * size * 4);
        this.fbo = new FBO(colormap_size, colormap_size);
        uploadAlphas(0, 0, size - 1, size - 1);
    }

    /**
     * Whether a generated rock or iron shades its cell. The generator adds a few near the start position after
     * drawing supply shadows, so those have none; a shaded cell reads at least half in the generated shadow map.
     */
    private static boolean castsShadow(@NonNull WorldInfo world_info, int @NonNull [] supply) {
        GLByteImage generated_shadow = world_info.blend_infos()[SHADOW_BLEND].getSourceImage();
        return generated_shadow.getPixel(supply[0], supply[1]) >= 128;
    }

    private @NonNull Texture newAlphaTexture() {
        return new Texture(size, size, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
    }

    /** Notes changed heights (inclusive rectangle, in cells); the texture follows on the next {@link #update}. */
    void heightsChanged(int x0, int y0, int x1, int y1) {
        alphas.heightsChanged(x0, y0, x1, y1);
        markDirty(x0, y0, x1, y1);
    }

    /**
     * Notes resources that came or went (inclusive rectangle, in cells): trees shade the ground around them, and
     * rocks and iron their own cell. The texture follows on the next {@link #update}.
     */
    void resourcesChanged(@NonNull ResourceLayer layer, int x0, int y0, int x1, int y1) {
        int reach = alphas.getSupplyShadowReach();
        int sx0 = x0 - reach;
        int sy0 = y0 - reach;
        int sx1 = x1 + reach;
        int sy1 = y1 + reach;
        List<int[]> shaded = new ArrayList<>();
        for (Resource kind : new Resource[]{Resource.ROCK, Resource.IRON})
            shaded.addAll(layer.positionsIn(kind, sx0, sy0, sx1, sy1));
        // Only generated rocks still where they were keep going without a shadow.
        shaded.removeIf(supply -> unshaded[supply[1] * size + supply[0]]);
        for (int y = Math.max(0, y0); y <= Math.min(size - 1, y1); y++)
            for (int x = Math.max(0, x0); x <= Math.min(size - 1, x1); x++)
                if (layer.get(x, y) != Resource.ROCK && layer.get(x, y) != Resource.IRON)
                    unshaded[y * size + x] = false;
        alphas.restampSupplies(layer.positionsIn(Resource.TREE, sx0 - reach, sy0 - reach, sx1 + reach, sy1 + reach),
                layer.positionsIn(Resource.PALM, sx0 - reach, sy0 - reach, sx1 + reach, sy1 + reach), shaded, sx0,
                sy0, sx1, sy1);
        markDirty(sx0, sy0, sx1, sy1);
    }

    private void markDirty(int x0, int y0, int x1, int y1) {
        dirty_x0 = Math.min(dirty_x0, x0);
        dirty_y0 = Math.min(dirty_y0, y0);
        dirty_x1 = Math.max(dirty_x1, x1);
        dirty_y1 = Math.max(dirty_y1, y1);
    }

    boolean hasChanges() {
        return !closed && dirty_x0 <= dirty_x1;
    }

    /** Redoes the texture where heights changed, keeping the island wide ranges as they were. */
    void update() {
        if (!hasChanges())
            return;
        int[] reach = alphas.reach(dirty_x0, dirty_y0, dirty_x1, dirty_y1);
        dirty_x0 = Integer.MAX_VALUE;
        dirty_y0 = Integer.MAX_VALUE;
        dirty_x1 = Integer.MIN_VALUE;
        dirty_y1 = Integer.MIN_VALUE;
        rebuild(reach[0], reach[1], reach[2], reach[3]);
    }

    /** Brings everything up to date once an edit is finished, rebuilding the whole island if its ranges moved. */
    void settle() {
        if (closed)
            return;
        update();
        if (alphas.rangesMoved())
            rebuild(0, 0, size - 1, size - 1);
    }

    /** Rebuilds the whole island's ground texture from the current heights. */
    void rebuildAll() {
        alphas.rangesMoved();
        rebuild(0, 0, size - 1, size - 1);
    }

    private void rebuild(int x0, int y0, int x1, int y1) {
        uploadAlphas(x0, y0, x1, y1);
        // Alphas are sampled bilinearly, so texels up to a cell away from a changed alpha change too.
        bake(Math.max(0, x0 - 1), Math.max(0, y0 - 1), Math.min(size - 1, x1 + 1), Math.min(size - 1, y1 + 1));
    }

    private void uploadAlphas(int x0, int y0, int x1, int y1) {
        upload_low.clear();
        upload_high.clear();
        alphas.compute(x0, y0, x1, y1, upload_low, upload_high);
        upload_low.flip();
        upload_high.flip();
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        context.setTexture(0, alpha_low);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, x1 - x0 + 1, y1 - y0 + 1, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, upload_low);
        context.setTexture(0, alpha_high);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, x1 - x0 + 1, y1 - y0 + 1, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, upload_high);
    }

    /** Re-layers the island texture over the given cells (inclusive), then refreshes its mipmaps. */
    private void bake(int x0, int y0, int x1, int y1) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            context.clearScissor();
            try (ScopedState _ = context.withBlendMode(BlendMode.NONE); ScopedState _ = context.withDepthMode(
                    DepthMode.NONE); ScopedState _ = context.withCullMode(
                            CullMode.NONE); ScopedState _ = context.withColorMask(true, true, true,
                                    true); ScopedState _ = shader.use()) {
                for (int i = 0; i < NUM_LAYERS; i++)
                    shader.setUniform("u_Layer[" + i + "]", i);
                shader.setUniform("u_AlphaLow", NUM_LAYERS);
                shader.setUniform("u_AlphaHigh", NUM_LAYERS + 1);
                shader.setUniform("u_Size", (float) colormap_size);
                shader.setUniform("u_TextureScale", texture_scale);
                shader.setUniform("u_LightColor", light_color[0], light_color[1], light_color[2]);
                context.setTexture(NUM_LAYERS, alpha_low);
                bindUntracked(NUM_LAYERS + 1, alpha_high.getHandle());

                fbo.bind();
                bakeInto(diffuse, diffuse_layers, true, x0, y0, x1, y1);
                bakeInto(normal, normal_layers, false, x0, y0, x1, y1);
                fbo.unbind();
                bindUntracked(NUM_LAYERS + 1, 0);
            } finally {
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            }
        }
        int tx0 = x0 * texels_per_cell;
        int ty0 = y0 * texels_per_cell;
        int tx1 = (x1 + 1) * texels_per_cell - 1;
        int ty1 = (y1 + 1) * texels_per_cell - 1;
        boolean mostly_whole = (long) (tx1 - tx0 + 1) * (ty1 - ty0 + 1) * 4 > (long) colormap_size * colormap_size;
        for (Texture target : new Texture[]{diffuse, normal}) {
            if (mostly_whole) {
                context.setTexture(0, target);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            } else {
                refreshMipmaps(target, tx0, ty0, tx1, ty1);
            }
        }
    }

    /**
     * Rebuilds the mipmaps over a rectangle of level 0 texels (inclusive). Regenerating the whole chain of an island
     * texture costs tens of milliseconds, too much to do while painting. Each level is a linear blit of the level
     * above at half size, which averages 2x2 texels like glGenerateMipmap's box filter.
     */
    private void refreshMipmaps(@NonNull Texture target, int x0, int y0, int x1, int y1) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mip_read_fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mip_draw_fbo);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        for (int level = 1, level_size = colormap_size >> 1; level_size > 0; level++, level_size >>= 1) {
            int dx0 = x0 >> 1;
            int dy0 = y0 >> 1;
            int dx1 = x1 >> 1;
            int dy1 = y1 >> 1;
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                    target.getHandle(), level - 1);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                    target.getHandle(), level);
            GL30.glBlitFramebuffer(2 * dx0, 2 * dy0, 2 * dx1 + 2, 2 * dy1 + 2, dx0, dy0, dx1 + 1, dy1 + 1,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
            x0 = dx0;
            y0 = dy0;
            x1 = dx1;
            y1 = dy1;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * Binds a texture on a unit the render context does not keep track of (it tracks the first eight), leaving the
     * active unit as the context believes it to be.
     */
    private static void bindUntracked(int unit, int handle) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, handle);
        GL13.glActiveTexture(active);
    }

    private void bakeInto(@NonNull Texture target, @NonNull Texture @NonNull [] layers, boolean is_diffuse, int x0,
            int y0, int x1, int y1) {
        // The target must not be bound for sampling while it is drawn into.
        context.setTexture(0, 0);
        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, target);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        fbo.checkStatus();
        for (int i = 0; i < NUM_LAYERS; i++)
            context.setTexture(i, layers[i]);
        shader.setUniform("u_Diffuse", is_diffuse);
        GL11.glViewport(x0 * texels_per_cell, y0 * texels_per_cell, (x1 - x0 + 1) * texels_per_cell,
                (y1 - y0 + 1) * texels_per_cell);
        quad.render();
    }

    @Override
    public void close() {
        // The editor keeps ticking while the screen fades out, so later calls must do nothing.
        closed = true;
        alpha_low.close();
        alpha_high.close();
        fbo.close();
        GL30.glDeleteFramebuffers(mip_read_fbo);
        GL30.glDeleteFramebuffers(mip_draw_fbo);
        shader.close();
        quad.close();
    }
}
