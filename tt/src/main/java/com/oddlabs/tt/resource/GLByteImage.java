package com.oddlabs.tt.resource;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public final class GLByteImage extends GLImage {

    @Override
    public int getPixelSize() {
        return Byte.BYTES;
    }

    public GLByteImage(int width, int height, @NonNull ByteBuffer pixel_data, int format) {
        super(width, height, pixel_data, format);
    }

    public GLByteImage(int width, int height, int format) {
        this(width, height, BufferUtils.createByteBuffer((width * height + 3) & ~3), format);
    }

    public GLByteImage(@NonNull Channel channel) {
        this(channel, GL11.GL_RED);
    }

    public GLByteImage(@NonNull Channel channel, int format) {
        this(channel.getWidth(), channel.getHeight(), format);
        float[] pixels = channel.getPixels();
        ByteBuffer buffer = getPixels();
        for (int i = 0; i < pixels.length; i++) {
            int pixel = Math.round(pixels[i] * 255);
            buffer.put(i, (byte) Math.clamp(pixel, 0, 255));
        }
    }

    @Override
    public @NonNull GLImage createImage(int width, int height, int format) {
        return new GLByteImage(width, height, format);
    }

    @Override
    public @NonNull GLImage createFromLayer(@NonNull Layer layer, int format) {
        Channel sourceChannel;
        if (format == GL11.GL_RED && layer.a != null) {
            sourceChannel = layer.a;
        } else {
            // For GL_LUMINANCE or other single-channel uses, default to red channel
            // A more accurate luminance calculation could be implemented here if needed.
            sourceChannel = layer.r;
        }
        return new GLByteImage(sourceChannel, format);
    }

    public @NonNull Channel toChannel() {
        Channel channel = new Channel(getWidth(), getHeight());
        float[] dest = channel.getPixels();
        ByteBuffer source = getPixels();
        for (int i = 0; i < dest.length; i++) {
            dest[i] = (source.get(i) & 0xff) / 255f;
        }
        return channel;
    }

    @Override
    public @NonNull GLImage scale(int newWidth, int newHeight) {
        return newWidth == getWidth() && newHeight == getHeight()
                ? this
                : new GLByteImage(toChannel().scale(newWidth, newHeight), getGLFormat());
    }

    @Override
    public int getPixel(int x, int y) {
        return getPixels().get(y * getWidth() + x) & 0xff;
    }

    @Override
    public void putPixel(int x, int y, int pixel) {
        getPixels().put(y * getWidth() + x, (byte) pixel);
    }
}
