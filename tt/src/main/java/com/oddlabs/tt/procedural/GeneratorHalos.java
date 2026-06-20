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
import java.util.EnumMap;

public final class GeneratorHalos extends TextureGenerator {
    public enum HaloType {
        SHADOWED,
        SELECTED
    }

    private final int size;
    private final float @NonNull [] @NonNull [] shadow_parms;
    private final float @NonNull [] @NonNull [] ring_parms;

    public GeneratorHalos(int size, float @NonNull [] @NonNull [] shadow_parms,
            float @NonNull [] @NonNull [] ring_parms) {
        this.size = size;
        this.shadow_parms = shadow_parms;
        this.ring_parms = ring_parms;
    }

    private @NonNull Channel generateLUT(int size, float @NonNull [] @NonNull [] gradient_list, boolean smooth) {
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
                value = smooth
                        ? Tools.interpolateSmooth(gradient_list[index - 1][1], gradient_list[index][1], fraction)
                        : Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1], fraction);
            }
            channel.putPixel(i, 0, value);
        }
        return channel;
    }

    @Override
    public Texture @NonNull [] generate() {
        Channel channel_shadow = generateLUT(size, shadow_parms, true);
        Channel channel_ring = generateLUT(size, ring_parms, false);
        Channel channel_black = new Channel(size, 1).fill(0f);
        Channel channel_white = new Channel(size, 1).fill(1f);

        // We use specialized channel mapping for DecalShader:
        // Red   = Ring Alpha
        // Green = Shadow Alpha
        // Blue  = 0
        // Alpha = 1 (Solid)

        EnumMap<HaloType, Layer> layers = new EnumMap<>(HaloType.class);

        // SHADOWED: Just the shadow in Green channel
        layers.put(HaloType.SHADOWED, new Layer(channel_black.copy(), channel_shadow.copy(), channel_black
                .copy(), channel_white.copy()));

        // SELECTED: Ring in Red, Shadow in Green
        layers.put(HaloType.SELECTED, new Layer(channel_ring.copy(), channel_shadow.copy(), channel_black
                .copy(), channel_white.copy()));

        Texture[] textures = new Texture[HaloType.values().length];
        for (HaloType type : HaloType.values()) {
            Layer layer = layers.get(type);
            int idx = type.ordinal();
            if (Landscape.DEBUG) new GLIntImage(layer).saveAsPNG("generator_halos_" + idx);
            textures[idx] = new Texture(new GLImage[]{new GLIntImage(layer)}, GL11.GL_RGBA8, GL11.GL_LINEAR,
                    GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        }
        return textures;
    }

    @Override
    public int hashCode() {
        return size * Arrays.deepHashCode(shadow_parms) * Arrays.deepHashCode(ring_parms);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return super.equals(o) &&
                o instanceof GeneratorHalos other &&
                size == other.size &&
                Arrays.deepEquals(shadow_parms, other.shadow_parms) &&
                Arrays.deepEquals(ring_parms, other.ring_parms);
    }
}
