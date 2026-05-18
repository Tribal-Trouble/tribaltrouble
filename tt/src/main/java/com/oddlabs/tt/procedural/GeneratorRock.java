package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class GeneratorRock extends TextureGenerator {
    private static final int TEXTURE_SIZE = 128;

    @Override
    public Texture @NonNull [] generate() {
        int seed = Globals.LANDSCAPE_SEED;
        Channel voronoi4 = new Voronoi(TEXTURE_SIZE, 4, 4, 1, 1f, seed).getDistance(0f, -1f, 1f);
        Channel voronoi8 = new Voronoi(TEXTURE_SIZE, 8, 8, 1, 1f, seed).getDistance(0f, -1f, 1f);
        Channel voronoi16 = new Voronoi(TEXTURE_SIZE, 16, 16, 1, 1f, seed).getDistance(0f, -1f, 1f);
        Channel noise8 = new Midpoint(TEXTURE_SIZE, 4, 0.5f, seed).toChannel();
        Channel noise256 = new Midpoint(TEXTURE_SIZE, 8, 0.5f, seed).toChannel();
        Channel perturb = noise8.copy().rotate(90);
        Channel empty = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(1f);
        Channel rock_bump1 = voronoi4.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.35f);
        Channel rock_bump2 = voronoi8.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
        Channel rock_bump3 = voronoi16.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.2f);
        Layer rock = new Layer(empty.copy(), empty.copy().brightness(0.8f), empty.copy().brightness(0.6f));
        rock.toHSV();
        rock.r = noise8.copy().dynamicRange(0.05f, 0.1f);
        rock.toRGB();
        Channel rock_bump = rock_bump1.channelAdd(rock_bump2).channelAdd(rock_bump3).channelAdd(noise256.multiply(
                0.4f));
        rock_bump.perturb(perturb, 0.1f);
        rock.bump(rock_bump, TEXTURE_SIZE / 144f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
        rock.gamma(1.25f);
        if (Landscape.DEBUG) new GLIntImage(rock).saveAsPNG("generator_rock");

        Channel mica = noise256.copy().gamma(0.5f).threshold(0.6f, 1.0f).multiply(0.8f);
        Layer normalMapLayer = rock_bump.toNormalMap(3.0f, mica);
        if (Landscape.DEBUG) new GLIntImage(normalMapLayer).saveAsPNG("generator_rock_normal");

        return new Texture[]{new Texture(new GLIntImage(rock), GL11.GL_RGB, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT), new Texture(new GLIntImage(normalMapLayer),
                        GL11.GL_RGB, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT)
        };
    }

    @Override
    public int hashCode() {
        return TEXTURE_SIZE;
    }
}
