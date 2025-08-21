package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.util.Image;
import com.oddlabs.util.Quad;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL11;

import java.net.URL;
import java.nio.ByteBuffer;

public final strictfp class Cursor {
    private final Texture texture;

    private final int offset_x;
    private final int offset_y;
    private final Quad cursor;
    private long glfwCursor = -1;

    public final void render(float x, float y) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
        GL11.glBegin(GL11.GL_QUADS);
        cursor.render(x - offset_x, y - offset_y);
        GL11.glEnd();
    }

    public Cursor(
            URL url_16_1,
            int offset_x_16_1,
            int offset_y_16_1,
            URL url_32_1,
            int offset_x_32_1,
            int offset_y_32_1,
            URL url_32_8,
            int offset_x_32_8,
            int offset_y_32_8) {
        this.offset_x = offset_x_32_8;
        this.offset_y = offset_y_32_8;
        Image image = Image.read(url_32_8);
        int width = image.getWidth();
        int height = image.getHeight();
        GLIntImage img_32_8 = new GLIntImage(width, height, image.getPixels(), GL11.GL_RGBA);

        Image image_16_1 = Image.read(url_16_1);
        GLIntImage img_16_1 =
                new GLIntImage(
                        image_16_1.getWidth(),
                        image_16_1.getHeight(),
                        image_16_1.getPixels(),
                        GL11.GL_RGBA);
        Image image_32_1 = Image.read(url_32_1);
        GLIntImage img_32_1 =
                new GLIntImage(
                        image_32_1.getWidth(),
                        image_32_1.getHeight(),
                        image_32_1.getPixels(),
                        GL11.GL_RGBA);

        texture =
                new Texture(
                        new GLImage[] {img_32_8},
                        GL11.GL_RGBA,
                        GL11.GL_NEAREST,
                        GL11.GL_NEAREST,
                        GL11.GL_REPEAT,
                        GL11.GL_REPEAT);
        cursor = new Quad(0, 0, 1, 1, 32, 32);

        // TODO: Lazy load this potentially?
        ByteBuffer buffer = img_32_8.getPixels();
        GLFWImage glfwImage = GLFWImage.malloc();
        // glfwCreateCursor expects the image data origin at top-left we're getting it from the
        // bottom left
        ByteBuffer flippedBuffer =
                flipVertically(buffer, img_32_8.getWidth(), img_32_8.getHeight());

        glfwImage.width(img_32_8.getWidth());
        glfwImage.height(img_32_8.getHeight());
        glfwImage.pixels(flippedBuffer);
        glfwCursor = GLFW.glfwCreateCursor(glfwImage, 0, 0);
    }

    public long getGlfwCursor() {
        return glfwCursor;
    }

    private static ByteBuffer flipVertically(ByteBuffer src, int width, int height) {
        int bytesPerPixel = 4;
        int stride = width * bytesPerPixel;
        ByteBuffer flipped = ByteBuffer.allocateDirect(src.capacity());
        for (int y = 0; y < height; y++) {
            int srcPos = (height - 1 - y) * stride;
            int dstPos = y * stride;
            for (int i = 0; i < stride; i++) {
                flipped.put(dstPos + i, src.get(srcPos + i));
            }
        }
        return flipped;
    }
}
