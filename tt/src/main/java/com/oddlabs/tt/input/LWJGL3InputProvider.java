package com.oddlabs.tt.input;

import com.oddlabs.tt.window.LWJGL3Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetInputMode;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursor;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

public final class LWJGL3InputProvider implements InputProvider<Long> {

    private final @NonNull LWJGL3Window window;
    private long windowHandle;

    // Keyboard State
    // @GuardedBy("this")
    private final Deque<@NonNull KeyEvent> keyEvents = new ArrayDeque<>();
    private @Nullable KeyEvent currentKeyEvent;

    // Mouse State
    private final Deque<@NonNull MouseEvent> mouseEvents = new ArrayDeque<>();
    private @Nullable MouseEvent currentMouseEvent;
    private double mouseX, mouseY;

    private static class KeyEvent {
        final int key;
        final int action; // GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT
        final int scancode;
        final int mods;
        char character;

        KeyEvent(int key, int action, int scancode, int mods) {
            this.key = key;
            this.action = action;
            this.scancode = scancode;
            this.mods = mods;
        }

        KeyEvent(int key, int action, int scancode, int mods, char character) {
            this(key, action, scancode, mods);
            this.character = character;
        }
    }

    private record MouseEvent(int button, boolean state, int x, int y, int dWheel) {
    }

    public LWJGL3InputProvider(@NonNull LWJGL3Window win) {
        this.window = win;
    }

    public void initCallbacks() {
        this.windowHandle = window.getHandle();
        if (windowHandle == MemoryUtil.NULL) {
             throw new IllegalStateException("Window handle is NULL. Window might not be created yet.");
        }

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            var event = new KeyEvent(key, action, scancode, mods);
            synchronized (keyEvents) {
                keyEvents.add(event);
            }
        });

        glfwSetCharCallback(windowHandle, (window, codepoint) -> {
            synchronized (keyEvents) {
                if (!keyEvents.isEmpty()) {
                    KeyEvent last = keyEvents.getLast();
                    if (last.action == GLFW_PRESS || last.action == GLFW_REPEAT) {
                        last.character = (char) codepoint;
                        return;
                    }
                }
                // No matching key event found, add a standalone char event
                keyEvents.add(new KeyEvent(0, GLFW_PRESS, 0, 0, (char) codepoint));
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
             float[] scale = this.window.getWindowContentScale();
             this.mouseX = xpos * scale[0];
             this.mouseY = this.window.getHeight() - (ypos * scale[1]) - 1; // Invert Y for OpenGL coords and scale
             synchronized (mouseEvents) {
                 mouseEvents.add(new MouseEvent(-1, false, (int)mouseX, (int)mouseY, 0));
             }
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
             synchronized (mouseEvents) {
                 mouseEvents.add(new MouseEvent(button, action == GLFW_PRESS, (int)mouseX, (int)mouseY, 0));
             }
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
             synchronized (mouseEvents) {
                 // wheel delta usually 120 per click in legacy? Or just +/- 1? LWJGL2 dWheel was usually +/- 120.
                 // GLFW gives floats. Let's say 120 * offset.
                 mouseEvents.add(new MouseEvent(-1, false, (int)mouseX, (int)mouseY, (int)(yoffset * 120)));
             }
        });
    }

    @Override
    public void close() {
        // Callbacks cleaned up by window destroy
    }

    @Override
    public void pollKeyboard() {
        // Events populated by callbacks
    }

    @Override
    public boolean nextKeyboardEvent() {
        synchronized (keyEvents) {
            if (keyEvents.isEmpty()) return false;
            currentKeyEvent = keyEvents.poll();
            return true;
        }
    }

    @Override
    public int getEventKey() {
        return currentKeyEvent != null ? currentKeyEvent.key : 0;
    }

    @Override
    public boolean getEventKeyState() {
        return currentKeyEvent != null && currentKeyEvent.action != GLFW_RELEASE;
    }

    @Override
    public int getEventKeyMods() {
        return currentKeyEvent != null ? currentKeyEvent.mods : 0;
    }

    @Override
    public char getEventCharacter() {
        return currentKeyEvent != null ? currentKeyEvent.character : 0;
    }

    @Override
    public boolean isRepeatEvent() {
        return currentKeyEvent != null && currentKeyEvent.action == GLFW_REPEAT;
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }

    @Override
    public void pollMouse() {
        // Populated by callbacks
    }

    @Override
    public boolean nextMouseEvent() {
        synchronized (mouseEvents) {
            if (mouseEvents.isEmpty()) return false;
            currentMouseEvent = mouseEvents.poll();
            return true;
        }
    }

    @Override
    public int getEventButton() {
        return currentMouseEvent != null ? currentMouseEvent.button() : -1;
    }

    @Override
    public boolean getEventButtonState() {
        return currentMouseEvent != null && currentMouseEvent.state();
    }

    @Override
    public int getEventDWheel() {
        return currentMouseEvent != null ? currentMouseEvent.dWheel() : 0;
    }

    @Override
    public int getEventX() {
        return currentMouseEvent != null ? currentMouseEvent.x() : (int)mouseX;
    }

    @Override
    public int getEventY() {
        return currentMouseEvent != null ? currentMouseEvent.y() : (int)mouseY;
    }

    @Override
    public int getMouseX() {
        return (int)mouseX;
    }

    @Override
    public int getMouseY() {
        return (int)mouseY;
    }

    @Override
    public boolean isButtonDown(int button) {
        return glfwGetMouseButton(windowHandle, button) == GLFW_PRESS;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        float[] scale = this.window.getWindowContentScale();
        // Convert OpenGL pixels back to screen coordinates
        // Y inversion: pixelY = height - screenY * scale - 1
        // screenY * scale = height - pixelY - 1
        // screenY = (height - pixelY - 1) / scale
        double screenX = x / (double)scale[0];
        double screenY = (this.window.getHeight() - y - 1) / (double)scale[1];
        glfwSetCursorPos(windowHandle, screenX, screenY);
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, grabbed ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    @Override
    public boolean isGrabbed() {
        return glfwGetInputMode(windowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
    }

    @Override
    public void setNativeCursor(@Nullable Long cursor) {
        // cursor object would be GLFW cursor handle (Long)
        if (null != cursor && cursor != MemoryUtil.NULL) {
            glfwSetCursor(windowHandle, cursor);
        } else {
            glfwSetCursor(windowHandle, MemoryUtil.NULL);
        }
    }
}