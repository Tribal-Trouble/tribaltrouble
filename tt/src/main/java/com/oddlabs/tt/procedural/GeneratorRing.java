package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Arrays;

public final class GeneratorRing extends TextureGenerator {
    private final int size;
    private final float[][] ring_parms;

    public GeneratorRing(int size, float[][] ring_parms) {
        this.size = size;
        this.ring_parms = ring_parms;
    }

    @Override
    public Texture @NonNull [] generate() {
        Channel channel_ring = new Ring(size, size, ring_parms, Ring.Interpolation.LINEAR).toChannel();
        Channel channel_white = new Channel(size, size).fill(1f);
        Layer layer = new Layer(channel_white.copy(), channel_white.copy(), channel_white.copy(), channel_ring);
        Texture[] textures = new Texture[1];
        textures[0] = new Texture(new GLImage[]{new GLIntImage(layer)}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        return textures;
    }

    @Override
    public int hashCode() {
        return size * Arrays.deepHashCode(ring_parms);
    }

    private static boolean equals(float @NonNull [] @NonNull [] a1, float @NonNull [] @NonNull [] a2) {
        if (a1.length != a2.length)
            return false;
        for (int i = 0; i < a1.length; i++) {
            if (!Arrays.equals(a1[i], a2[i]))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!super.equals(o))
            return false;
        GeneratorRing other = (GeneratorRing) o;
        return size == other.size && equals(ring_parms, other.ring_parms);
    }
}
