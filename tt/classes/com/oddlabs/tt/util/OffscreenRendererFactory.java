package com.oddlabs.tt.util;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;


public final class OffscreenRendererFactory {
	private final static int PBUFFER_FAILURE_MAX = 2;
	private int pbuffer_failures;

	public OffscreenRenderer createRenderer(int width, int height, PixelFormat format, boolean use_copyteximage, boolean use_pbuffer, boolean use_fbo) {
		OffscreenRenderer renderer = doCreateRenderer(width, height, format, use_copyteximage, use_pbuffer, use_fbo);
System.out.println("Creating renderer = " + renderer);
		return renderer;
	}

	public OffscreenRenderer doCreateRenderer(int width, int height, PixelFormat format, boolean use_copyteximage, boolean use_pbuffer, boolean use_fbo) {
		if (use_pbuffer && pbuffer_failures < PBUFFER_FAILURE_MAX) {
			try {
				return new PbufferRenderer(width, height, format, use_copyteximage, this);
			} catch (LWJGLException e) {
				System.out.println("Failed to create PbufferRenderer: " + e);
			}
		}
		if (use_fbo) {
			try {
				return new FramebufferTextureRenderer(width, height, format.getAlphaBits() > 0, use_copyteximage);
			} catch (Exception e) {
				System.out.println("Failed to create FramebufferRenderer: " + e);
			}
		}
		return new BackBufferRenderer(width, height, use_copyteximage);
	}

	void pbufferFailed() {
		pbuffer_failures++;
	}
}
