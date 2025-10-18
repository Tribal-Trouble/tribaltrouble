package com.oddlabs.tt.util;

import com.oddlabs.tt.render.Renderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class FramebufferTextureRenderer extends OffscreenRenderer {
	private final int fb_id;
	private final int rb_id;

	protected FramebufferTextureRenderer(int width, int height, boolean has_alpha, boolean use_copyteximage) throws Exception {
		super(width, height, use_copyteximage);
		pushGLState();
		IntBuffer tmp = BufferUtils.createIntBuffer(1);
		EXTFramebufferObject.glGenFramebuffersEXT(tmp);
		fb_id = tmp.get(0);
		assert fb_id != 0;
		GL11.glGenTextures(tmp);
		rb_id = tmp.get(0);
		assert rb_id != 0;
		int internal_format = has_alpha ? GL11.GL_RGBA8 : GL11.GL_RGB8;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, rb_id);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internal_format, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fb_id);
		EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, rb_id, 0);
		int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
		if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
			assert status == EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT: status;
			deleteBuffers();
			throw new Exception("Failed to setup FBO");
		}
		GL11.glDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
		GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
		Renderer.dumpWindowInfo();
		init();
	}

        @Override
	public boolean isLost() {
		return false;
	}

	private void deleteBuffers() {
		IntBuffer tmp = BufferUtils.createIntBuffer(4);
		tmp.put(0, fb_id);
		EXTFramebufferObject.glDeleteFramebuffersEXT(tmp);
		tmp.put(0, rb_id);
		GL11.glDeleteTextures(tmp);
	}

        @Override
	protected void finish() {
		deleteBuffers();
		popGLState();
	}
}
