package com.oddlabs.tt.util;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.logging.Logger;

public abstract class OffscreenRenderer {
	private static final Logger logger = Logger.getLogger(OffscreenRenderer.class.getName());
	private final int width;
	private final int height;
	private final @Nullable GLImage image;

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	protected OffscreenRenderer(int width, int height) {
		this.width = width;
		this.height = height;
		image = new GLIntImage(width, height, GL11.GL_RGBA);
	}

	protected final void init() {
		Renderer.initGL();
		GL11.glViewport(0, 0, width, height);
	}

	public final void dumpToFile(String filename) {
		GLIntImage image = new GLIntImage(width, height, GL11.GL_RGBA);
		GL11.glReadPixels(0, 0, image.getWidth(), image.getHeight(), image.getGLFormat(), image.getGLType(), image.getPixels());
		logger.info("Dumping offscreen buffer to file: " + filename);
		com.oddlabs.util.Utils.flip(image.getPixels(), image.getWidth()*4, image.getHeight());
		image.saveAsPNG(filename);
	}

	public final void copyToTexture(@NonNull Texture tex, int mip_level, int format, int x0, int y0, int x1, int y1) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getHandle());
		assert x0 >= 0 && y0 >= 0 && x1 <= width && y1 <= height && image != null;
		GL11.glReadPixels(x0, y0, x1, y1, image.getGLFormat(), image.getGLType(), image.getPixels());
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, mip_level, format, x1 - x0, y1 - y0, 0, image.getGLFormat(), image.getGLType(), image.getPixels());
	}

	public final boolean destroy() {
		finish();
		return true;
	}

	protected abstract void finish();
}
