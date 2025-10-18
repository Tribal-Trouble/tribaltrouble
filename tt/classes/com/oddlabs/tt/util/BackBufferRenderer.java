package com.oddlabs.tt.util;

import org.lwjgl.opengl.Display;

public final class BackBufferRenderer extends OffscreenRenderer {
	private static boolean back_buffer_dirty = false;

	public static boolean isBackBufferDirty() {
		boolean result = back_buffer_dirty;
		back_buffer_dirty = false;
		return result;
	}

	protected BackBufferRenderer(int width, int height, boolean use_copyteximage) {
		super(width, height, use_copyteximage);
		pushGLState();
		back_buffer_dirty = true;
		Display.isDirty();
		init();
	}

    @Override
	public boolean isLost() {
		return Display.isDirty();
	}

    @Override
	protected void finish() {
		popGLState();
	}
}
