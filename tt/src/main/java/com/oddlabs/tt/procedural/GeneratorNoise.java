package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * A texture generator that produces Perlin noise.
 * Used for organic turbulence effects in shaders.
 */
public final class GeneratorNoise extends TextureGenerator {
    private final int size;
    private final long seed;

    public GeneratorNoise(int size, long seed) {
        this.size = size;
        this.seed = seed;
    }

    @Override
    public @NonNull Texture @NonNull [] generate() {
        Perlin perlin = new Perlin(size, size, 4, 4, 0.5f, 4, seed, Perlin.Interpolation.SMOOTH,
                Perlin.Summation.NORMAL);
        Layer layer = perlin.toLayer();

        return new Texture[]{
                new Texture(new GLIntImage(layer), GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT,
                        GL11.GL_REPEAT),
        };
    }

    @Override
    public int hashCode() {
        return size + (int) seed;
    }
}
