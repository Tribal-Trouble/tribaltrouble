package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

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
         * @param yHot y location from top left of cursor hot spot
         */
        NativeCursor(@NonNull GLImage image, int xHot, int yHot) {
            int width = image.getWidth();
            int height = image.getHeight();

            long nativeCursor;
            try (MemoryStack stack = MemoryStack.stackPush()) {

                GLFWImage glfwImage = GLFWImage.malloc(stack);
                glfwImage.set(width, height, image.getPixels());
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
     * @param yHot y location from top left of cursor hot spot
     */
    public Cursor(@NonNull GLImage image, int xHot, int yHot) {
        super(new NativeCursor(image, xHot, yHot));
    }

    @Override
    public void close() {
        Renderer.getLocalInput().getPointerInput().deletingCursor(this);
        super.close();
    }

    public long getCursor() {
		return state.cursor;
	}
}
