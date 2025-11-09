package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class GeneratorLightning extends TextureGenerator {
	private static final int TEXTURE_SIZE = 128;
	@Override
	public Texture @NonNull [] generate() {
		Channel gradient = new Gradient(TEXTURE_SIZE, 1, new float[][]{{0f, 0f}, {.47f, .25f}, {.5f, 1f}, {.53f, .25f}, {1f, 0f}}, Gradient.HORIZONTAL, Gradient.SMOOTH).toChannel();
		Layer layer = new Layer(new Channel(TEXTURE_SIZE, 1).fill(1f), new Channel(TEXTURE_SIZE, 1).fill(1f), gradient.copy(), gradient.copy());
		GLIntImage img = new GLIntImage(layer);
		if (Landscape.DEBUG) img.saveAsPNG("generator_lightning");
		Texture[] textures = new Texture[1];
		textures[0] = new Texture(img.createMipMaps(), Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
		return textures;
	}

	@Override
	public int hashCode() {
		return TEXTURE_SIZE + 3346;
	}
}
