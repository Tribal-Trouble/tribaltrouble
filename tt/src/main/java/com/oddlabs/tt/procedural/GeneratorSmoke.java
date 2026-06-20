package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Objects;

/**
 * Procedural generator for stylized "cartoon" smoke and dust "puffs".
 * Uses Voronoi noise for chunky structures while maintaining soft edges for volumetric blending.
 */
public final class GeneratorSmoke extends TextureGenerator {
    private static final int TEXTURE_SIZE = 128;

    private final int seed;
    private final float baseBrightness;
    private final float alphaMultiplier;

    /**
     * Standard smoke.
     */
    public GeneratorSmoke() {
        this(42, 0.6f, 1.0f);
    }

    /**
     * Parameterized constructor for specialized effects.
     *
     * @param seed random seed for noise.
     * @param baseBrightness base grayscale value [0, 1].
     * @param alphaMultiplier scaling factor for the alpha channel.
     */
    public GeneratorSmoke(int seed, float baseBrightness, float alphaMultiplier) {
        this.seed = seed;
        this.baseBrightness = baseBrightness;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public @NonNull Texture @NonNull [] generate() {
        Channel voronoi = new Voronoi(TEXTURE_SIZE, 4, 4, 1, 1f, seed).getDistance(-1f, 1f, 0f);

        // Ultra-Soft Alpha Profile (Radius 0.4, Gamma 3.0).
        // This ensures the "cartoon chunks" fade out before hitting the sprite edge, preventing banding.
        Channel smoke_alpha = new Ring(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 1f}, {0.4f, 0f}},
                Ring.Interpolation.SMOOTH).toChannel().gamma(3.0f);

        // Perturb the alpha with the Voronoi noise to create stylized chunky edges.
        smoke_alpha.channelMultiply(voronoi.copy().dynamicRange(0.85f, 1.0f));

        if (alphaMultiplier != 1.0f) {
            smoke_alpha.brightness(alphaMultiplier);
        }

        Channel smoke_color = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(baseBrightness);

        // Smooth the bump map significantly to keep the shapes "bloby" but not "ringy".
        Channel smoke_bump = voronoi.copy().gamma(0.5f).smooth(3).dynamicRange(0.0f, 1.0f).channelMultiply(smoke_alpha);

        smoke_color.bump(smoke_bump, 0.5f, -0.5f, 0.15f, 0.4f, 0.7f);

        Layer smoke = new Layer(smoke_color.copy(), smoke_color.copy(), smoke_color.copy(), smoke_alpha);

        GLIntImage smoke_img = new GLIntImage(smoke);
        if (Landscape.DEBUG) smoke_img.saveAsPNG("generator_smoke_" + seed);

        return new Texture[]{
                new Texture(smoke_img, Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                        GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE)
        };
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof GeneratorSmoke other && seed == other.seed
                && Float.compare(baseBrightness, other.baseBrightness) == 0
                && Float.compare(alphaMultiplier, other.alphaMultiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seed, baseBrightness, alphaMultiplier);
    }
}
