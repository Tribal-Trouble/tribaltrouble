package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public abstract class BlendInfo {
	private final @NonNull Texture alpha_map;
	private final GLByteImage sourceImage;

	private @NonNull Texture createAlphaMap(GLByteImage alpha_image, int format) {
        GLImage[] mipmaps = alpha_image.buildMipMaps(0, 1.0f, true, false);
		return new Texture(mipmaps, format, GL11.GL_LINEAR_MIPMAP_LINEAR,
										  GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
	}

	protected BlendInfo(GLByteImage alpha_image, int format) {
		this.sourceImage = alpha_image;
		alpha_map = createAlphaMap(alpha_image, format);
	}

	public @NonNull Texture getAlphaMap() {
		return alpha_map;
	}

	public GLByteImage getSourceImage() {
		return sourceImage;
	}

/*	public void delete() {
		alpha_map.delete();
	}
*/
	protected final void bindAlpha() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, alpha_map.getHandle());
	}

	public abstract void setup();
	public abstract void reset();
}
