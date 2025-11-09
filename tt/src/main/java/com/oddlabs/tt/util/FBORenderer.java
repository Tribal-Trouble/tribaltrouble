package com.oddlabs.tt.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * An offscreen renderer that uses a Framebuffer Object (FBO) to render directly to a texture.
 */
public final class FBORenderer extends OffscreenRenderer {
	private final int framebuffer_id;
	private final int render_texture_id;

	public FBORenderer(int width, int height) {
		super(width, height);
		pushGLState();

		// Create and bind the framebuffer
		framebuffer_id = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer_id);

		// Create the texture that will be rendered to
		render_texture_id = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, render_texture_id);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		// Attach the texture to the FBO
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, render_texture_id, 0);

		// Check for FBO completeness
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
			deleteBuffers();
			throw new RuntimeException("Failed to setup FBO, status: " + status);
		}

		// Set the draw and read buffers
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

		init();
	}

	private void deleteBuffers() {
		GL30.glDeleteFramebuffers(framebuffer_id);
		GL11.glDeleteTextures(render_texture_id);
	}

	@Override
	protected void finish() {
		deleteBuffers();
		popGLState();
	}
}