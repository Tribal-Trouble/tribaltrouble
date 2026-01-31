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

public final class GeneratorHalos extends TextureGenerator {
	public static final int SHADOWED = 0;
	public static final int SELECTED = 1;
	private final int size;
	private final float @NonNull [] @NonNull [] shadow_parms;
	private final float @NonNull [] @NonNull [] ring_parms;

	public GeneratorHalos(int size, float @NonNull [] @NonNull [] shadow_parms, float @NonNull [] @NonNull [] ring_parms) {
		this.size = size;
		this.shadow_parms = shadow_parms;
		this.ring_parms = ring_parms;
	}

	@Override
	public Texture @NonNull [] generate() {
		Channel channel_shadow = new Ring(size, size, shadow_parms, Ring.Interpolation.SMOOTH).toChannel();
		Channel channel_ring = new Ring(size, size, ring_parms, Ring.Interpolation.LINEAR).toChannel();
		Channel channel_black = new Channel(size, size).fill(0f);
		Channel channel_white = new Channel(size, size).fill(1f);
		Layer[] layers = new Layer[2];
		layers[SHADOWED] = new Layer(channel_black, channel_black, channel_black, channel_shadow);
		layers[SELECTED] = new Layer(channel_white.copy(), channel_white.copy(), channel_white.copy(), channel_ring);
		layers[SELECTED] = layers[SHADOWED].copy().layerBlend(layers[SELECTED]);
		Texture[] textures = new Texture[layers.length];
		for (int i = 0; i < layers.length; i++) {
			if (Landscape.DEBUG) new GLIntImage(layers[i]).saveAsPNG("generator_halos_" + i);
			textures[i] = new Texture(new GLImage[]{new GLIntImage(layers[i])}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
		}
		return textures;
	}

	@Override
	public int hashCode() {
		return size*Arrays.deepHashCode(shadow_parms)*Arrays.deepHashCode(ring_parms);
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
