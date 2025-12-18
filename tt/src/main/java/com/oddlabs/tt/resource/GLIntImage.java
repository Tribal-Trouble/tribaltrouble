package com.oddlabs.tt.resource;

import com.oddlabs.procedural.Layer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

public final class GLIntImage extends GLImage {
	private final @NonNull IntBuffer pixels;

	@Override
	public int getPixelSize() {
		return Integer.BYTES;
	}

	public @NonNull IntBuffer getIntPixels() {
		return pixels;
	}

	public GLIntImage(int width, int height, @NonNull ByteBuffer pixel_data, int format) {
		super(width, height, pixel_data, format);
		pixels = pixel_data.asIntBuffer();
	}

	public GLIntImage(int width, int height, int format) {
		this(width, height, Objects.requireNonNull(BufferUtils.createByteBuffer(width*height*Integer.BYTES)), format);
	}

	public GLIntImage(@NonNull Layer layer) {
		this(layer.getWidth(), layer.getHeight(), GL11.GL_RGBA);
		for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int ri = ((int)(layer.r.getPixel(x, y)*255 + .5f)) & 0xff;
                int gi = ((int)(layer.g.getPixel(x, y)*255 + .5f)) & 0xff;
                int bi = ((int)(layer.b.getPixel(x, y)*255 + .5f)) & 0xff;
                int ai;
                if (layer.a != null) {
                    ai = ((int)(layer.a.getPixel(x, y)*255 + .5f)) & 0xff;
                } else {
                    ai = 255;
                }
                /*				if (ri < 0) ri = 0;
                if (gi < 0) gi = 0;
                if (bi < 0) bi = 0;
                if (ai < 0) ai = 0;
                if (ri > 255) ri = 255;
                if (gi > 255) gi = 255;
                if (bi > 255) bi = 255;
                if (ai > 255) ai = 255;*/
                int pixel = (ai << 24) | (bi << 16) | (gi << 8) | ri;
                putPixel(x, y, pixel);
            }
        }
	}

	@Override
	public @NonNull GLImage createImage(int width, int height, int format) {
		return new GLIntImage(width, height, format);
	}

        @Override
        public @NonNull GLImage createFromLayer(@NonNull Layer layer, int format) {
            return new GLIntImage(layer);
        }

	@Override
	public int getPixel(int x, int y) {
		return pixels.get(y*getWidth() + x);
	}

	@Override
	public void putPixel(int x, int y, int pixel) {
		pixels.put(y*getWidth() + x, pixel);
	}
}
