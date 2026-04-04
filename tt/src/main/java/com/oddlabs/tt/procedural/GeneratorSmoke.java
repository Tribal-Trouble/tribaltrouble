package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class GeneratorSmoke extends TextureGenerator {
    private static final int TEXTURE_SIZE = 128;

    @Override
    public @NonNull Texture @NonNull [] generate() {
        Channel voronoi = new Voronoi(TEXTURE_SIZE, 4, 4, 1, 1f, 42).getDistance(-1f, 0f, 0f);
        Channel smoke_alpha = new Ring(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 1f}, {0.5f, 0f}}, Ring.Interpolation.SMOOTH).toChannel().gamma(1.5f);
        Channel smoke_color = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(0.5f);
        Channel smoke_bump = voronoi.gamma(0.25f).smooth(3).smooth(1).dynamicRange(0.925f, 1f).channelMultiply(smoke_alpha);
        smoke_color.bump(smoke_bump, 0f, -4f, 0f, 1f, 0f);
        Layer smoke = new Layer(smoke_alpha.copy(), smoke_alpha.copy(), smoke_alpha.copy(), smoke_alpha);
        GLIntImage smoke_img = new GLIntImage(smoke);
        if (Landscape.DEBUG) smoke_img.saveAsPNG("generator_smoke");
        return new Texture[]{
                new Texture(smoke_img, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE),
        };
    }

    @Override
    public int hashCode() {
        return TEXTURE_SIZE + 3;
    }
}
