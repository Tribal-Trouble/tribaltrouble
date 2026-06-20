package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;
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

    private @NonNull Channel generateLUT(int size, float @NonNull [] @NonNull [] gradient_list) {
        Channel channel = new Channel(size, 1);
        int index_max = gradient_list.length - 1;
        for (int i = 0; i < size; i++) {
            float pos = (i + 0.5f) / size; // 0..1
            // gradient_list uses radius 0..0.5
            float radius = pos * 0.5f;

            int index = 0;
            while (radius >= gradient_list[index][0] && index < index_max) {
                index++;
            }

            float value;
            if (radius < gradient_list[0][0]) {
                value = gradient_list[0][1];
            } else if (radius >= gradient_list[index_max][0]) {
                value = gradient_list[index_max][1];
            } else {
                float fraction = (radius - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index
                        - 1][0]);
                value = Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1], fraction);
            }
            channel.putPixel(i, 0, value);
        }
        return channel;
    }

    @Override
    public Texture @NonNull [] generate() {
        Channel channel_ring = generateLUT(size, ring_parms);
        Channel channel_black = new Channel(size, 1).fill(0f);
        Channel channel_white = new Channel(size, 1).fill(1f);

        // Ring only: Red channel for DecalShader
        Layer layer = new Layer(channel_ring.copy(), channel_black.copy(), channel_black.copy(), channel_white.copy());

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
