package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Procedurally generates a Voronoi-based crack texture used for ground-impact decals.
 * The output is a single RGBA texture whose alpha encodes crack borders and whose
 * red/green channels provide an orange-tinted radial falloff.
 */
public final class GeneratorCrack extends TextureGenerator {
    private static final int TEXTURE_SIZE = 128;

    @Override
    public @NonNull Texture @NonNull [] generate() {
        int seed = Globals.LANDSCAPE_SEED;
        Channel voronoi = new Voronoi(TEXTURE_SIZE, 5, 5, 1, 1f, seed).getDistance(-1f, 1f, 0f);
        Channel borders = voronoi.dynamicRange().threshold(0.0f, 0.05f);
        Channel falloff = new Ring(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 1f}, {0.35f, 1f}, {0.5f, 0f}},
                Ring.Interpolation.SMOOTH).toChannel();
        Channel alpha = borders.channelMultiply(falloff);
        Channel red = falloff.copy();
        Channel green = falloff.copy().multiply(0.25f);
        Channel blue = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(0f);
        Layer layer = new Layer(red, green, blue, alpha);
        return new Texture[]{
                new Texture(new GLIntImage(layer), GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                        GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE),
        };
    }

    @Override
    public int hashCode() {
        return TEXTURE_SIZE + 9876;
    }
}
