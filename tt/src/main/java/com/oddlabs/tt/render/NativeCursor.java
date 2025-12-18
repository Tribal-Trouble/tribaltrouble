package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwCreateCursor;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;

public final class NativeCursor extends NativeResource<NativeCursor.Cursor> {
    static final class Cursor extends NativeResource.NativeState {
        private final long cursor;

        Cursor(@NonNull GLIntImage image_16_1, int offset_x_16_1, int offset_y_16_1,
               @NonNull GLIntImage image_32_1, int offset_x_32_1, int offset_y_32_1,
               @NonNull GLIntImage image_32_8, int offset_x_32_8, int offset_y_32_8) {
            
            // Prefer 32x32 with 8-bit alpha if available (standard nowadays)
            GLIntImage source = image_32_8;
            int xHot = offset_x_32_8;
            int yHot = offset_y_32_8;

            int width = source.getWidth();
            int height = source.getHeight();
            // Invert hotspot Y because GLFW coordinates are from top-left, while our source hotspot is from bottom-left
            yHot = height - 1 - yHot;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer pixels = stack.malloc(width * height * 4);
                IntBuffer srcPixels = source.getIntPixels();
                
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

                GLFWImage image = GLFWImage.malloc(stack);
                image.set(width, height, pixels);
                
                this.cursor = glfwCreateCursor(image, xHot, yHot);
            }
        }

        @Override
        public void close() {
            if (cursor != MemoryUtil.NULL) {
                PointerInput.deletingCursor(cursor);
                glfwDestroyCursor(cursor);
            }
        }
    }

    public NativeCursor(@NonNull GLIntImage image_16_1, int offset_x_16_1, int offset_y_16_1,
                        @NonNull GLIntImage image_32_1, int offset_x_32_1, int offset_y_32_1,
                        @NonNull GLIntImage image_32_8, int offset_x_32_8, int offset_y_32_8) {
        super(new NativeCursor.Cursor(image_16_1, offset_x_16_1, offset_y_16_1,
                image_32_1, offset_x_32_1, offset_y_32_1,
                image_32_8, offset_x_32_8, offset_y_32_8));
    }

	public long getCursor() {
		return state.cursor;
	}

	public boolean setActive() {
		if (Settings.getSettings().use_native_cursor && state.cursor != MemoryUtil.NULL) {
			PointerInput.setActiveCursor(state.cursor);
			return true;
		} else {
			PointerInput.setActiveCursor(MemoryUtil.NULL);
			return false;
		}
	}
}
