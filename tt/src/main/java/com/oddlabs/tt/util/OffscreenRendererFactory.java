package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;


public final class OffscreenRendererFactory {

    public @NonNull OffscreenRenderer createRenderer(int width, int height, @NonNull PixelFormat format) throws RuntimeException {
        OffscreenRenderer renderer = new FramebufferTextureRenderer(width, height, format.getAlphaBits() > 0);
        System.out.println("Creating renderer = " + renderer);
        return renderer;
    }
}
