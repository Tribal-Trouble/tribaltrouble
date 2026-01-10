package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;
/** GLFW Cursor */
public final class Cursor extends NativeResource<Cursor.NativeCursor> {
    public static final Cursor NULL_CURSOR = new Cursor(MemoryUtil.NULL);
    static final class NativeCursor extends NativeResource.NativeState {
        private final long cursor;

        NativeCursor(long cursor) {
            this.cursor = cursor;
        }

        /** Create a new cursor instance from an image
         *
         * @param image source cursor image
         * @param xHot x location from top left of cursor hot spot
         * @param yHot y location from bottom left of cursor hot spot
         */
        NativeCursor(@NonNull Image image, int xHot, int yHot) {
            int width = image.getWidth();
            int height = image.getHeight();

            long nativeCursor;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer pixels = stack.malloc(width * height * 4);
                IntBuffer srcPixels = image.getPixels().asIntBuffer();
                
                // Flip image vertically: Read from top row (height-1) down to bottom row (0)
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int srcIndex = ((height - 1 - y) * width) + x;
                        int p = srcPixels.get(srcIndex);

                        byte r = (byte) (p >> 24);
                        byte g = (byte) (p >> 16);
                        byte b = (byte) (p >> 8);
                        byte a = (byte) p;
                        
                        pixels.put(r);
                        pixels.put(g);
                        pixels.put(b);
                        pixels.put(a);
                    }
                }
                pixels.flip();

                GLFWImage glfwImage = GLFWImage.malloc(stack);
                glfwImage.set(width, height, pixels);
                // Invert hotspot Y because GLFW coordinates are from top-left, while our source hotspot is from bottom-left
                yHot = height - 1 - yHot;
                nativeCursor = glfwCreateCursor(glfwImage, xHot, yHot);
            }

            this(nativeCursor);
        }

        @Override
        public void close() {
            if (cursor != MemoryUtil.NULL) {
                glfwDestroyCursor(cursor);
            }
        }
    }

    private Cursor(long nativeCursor) {
        super(new NativeCursor(nativeCursor));
    }

    /** Create a new cursor instance from an image
     *
     * @param image source cursor image
     * @param xHot x location from top left of cursor hot spot
     * @param yHot y location from lower left of cursor hot spot
     */
    public Cursor(@NonNull Image image, int xHot, int yHot) {
        super(new NativeCursor(image, xHot, yHot));
    }

    @Override
    public void close() {
        PointerInput.deletingCursor(this);
        super.close();
    }

    public long getCursor() {
		return state.cursor;
	}

	public void setActive() {
		PointerInput.setActiveCursor(this);
	}
}
