package com.oddlabs.tt.input;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.Display;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.LinkedList;
import java.util.Queue;

public final strictfp class Mouse {

    private static boolean created = false;
    private static boolean grabbed = false;
    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int lastMouseX = 0;
    private static int lastMouseY = 0;
    private static int dX = 0;
    private static int dY = 0;
    private static int dWheel = 0;
    private static boolean[] buttonStates = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

    // Event queue
    private static Queue<MouseEvent> eventQueue = new LinkedList<>();
    private static MouseEvent currentEvent = null;

    public static class MouseEvent {

        public final int x, y, dx, dy, dWheel, button;
        public final boolean buttonState;
        public final EventType type;

        public MouseEvent(
                int x,
                int y,
                int dx,
                int dy,
                int dWheel,
                int button,
                boolean buttonState,
                EventType type) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.dWheel = dWheel;
            this.button = button;
            this.buttonState = buttonState;
            this.type = type;
        }
    }

    public enum EventType {
        MOVE,
        BUTTON,
        SCROLL
    }

    public static void create() {
        if (created) {
            return;
        }
        long window = Display.getWindow();
        if (window == 0) {
            throw new IllegalStateException("Display must be created before Mouse");
        }

        GLFW.glfwSetCursorPosCallback(
                window,
                new GLFWCursorPosCallback() {
                    @Override
                    public void invoke(long win, double xpos, double ypos) {
                        int ix = (int) xpos;
                        int iy = Display.getHeight() - (int) ypos;
                        if (!Settings.getSettings().getNativeCursor()) {
                            // Clamp coordinates to stay within game bounds
                            int clampedX = Math.max(0, Math.min(ix, Display.getWidth() - 1));
                            int clampedY = Math.max(0, Math.min(iy, Display.getHeight() - 1));

                            // If coordinates were clamped, reset GLFW cursor position to prevent
                            // drift
                            if (clampedX != ix || clampedY != iy) {
                                // Convert back to GLFW coordinate space and reset cursor
                                double newGlfwX = clampedX;
                                double newGlfwY = Display.getHeight() - clampedY;
                                GLFW.glfwSetCursorPos(win, newGlfwX, newGlfwY);
                            }

                            ix = clampedX;
                            iy = clampedY;
                        }

                        // System.out.println("GLFW raw: (" + xpos + ", " + ypos + ") -> Processed:
                        // (" + ix + ", " + iy + ") DisplaySize: (" + Display.getWidth() + ", " +
                        // Display.getHeight() + ")");
                        dX = ix - mouseX;
                        dY = iy - mouseY;
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                        mouseX = ix;
                        mouseY = iy;
                        // System.out.println("Mouse moved to: (" + mouseX + ", " + mouseY + ")
                        // Delta: (" + dX + ", " + dY + ")");
                        eventQueue.offer(
                                new MouseEvent(
                                        mouseX, mouseY, dX, dY, 0, -1, false, EventType.MOVE));
                    }
                });
        GLFW.glfwSetMouseButtonCallback(
                window,
                new GLFWMouseButtonCallback() {
                    @Override
                    public void invoke(long win, int button, int action, int mods) {
                        if (button < 0 || button >= buttonStates.length) {
                            return;
                        }
                        boolean pressed = action == GLFW.GLFW_PRESS;
                        buttonStates[button] = pressed;
                        eventQueue.offer(
                                new MouseEvent(
                                        mouseX,
                                        mouseY,
                                        0,
                                        0,
                                        0,
                                        button,
                                        pressed,
                                        EventType.BUTTON));
                    }
                });
        GLFW.glfwSetScrollCallback(
                window,
                new GLFWScrollCallback() {
                    @Override
                    public void invoke(long win, double xoffset, double yoffset) {
                        dWheel = (int) (yoffset * 100);
                        eventQueue.offer(
                                new MouseEvent(
                                        mouseX, mouseY, 0, 0, dWheel, -1, false, EventType.SCROLL));
                    }
                });
        created = true;
    }

    public static void destroy() {
        created = false;
        eventQueue.clear();
    }

    public static boolean isCreated() {
        return created;
    }

    public static void poll() {
        // GLFW handles polling in Display.update(), but we reset deltas here
        dX = 0;
        dY = 0;
        dWheel = 0;
    }

    public static boolean next() {
        if (eventQueue.isEmpty()) {
            currentEvent = null;
            return false;
        }
        currentEvent = eventQueue.poll();
        return true;
    }

    public static int getEventX() {
        return currentEvent != null ? currentEvent.x : mouseX;
    }

    public static int getEventY() {
        return currentEvent != null ? currentEvent.y : mouseY;
    }

    public static int getEventDX() {
        return currentEvent != null ? currentEvent.dx : 0;
    }

    public static int getEventDY() {
        return currentEvent != null ? currentEvent.dy : 0;
    }

    public static int getEventDWheel() {
        return currentEvent != null ? currentEvent.dWheel : 0;
    }

    public static int getEventButton() {
        return currentEvent != null ? currentEvent.button : -1;
    }

    public static boolean getEventButtonState() {
        return currentEvent != null && currentEvent.type == EventType.BUTTON
                ? currentEvent.buttonState
                : false;
    }

    public static int getX() {
        return mouseX;
    }

    public static int getY() {
        return mouseY;
    }

    public static int getDX() {
        return dX;
    }

    public static int getDY() {
        return dY;
    }

    public static int getDWheel() {
        return dWheel;
    }

    public static boolean isButtonDown(int button) {
        return button >= 0 && button < buttonStates.length && buttonStates[button];
    }

    public static void setGrabbed(boolean grab) {
        grabbed = grab;
        long window = Display.getWindow();
        if (window != 0) {
            GLFW.glfwSetInputMode(
                    window,
                    GLFW.GLFW_CURSOR,
                    grab ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    public static boolean isGrabbed() {
        return grabbed;
    }

    public static void setCursorPosition(int x, int y) {
        long window = Display.getWindow();
        if (window != 0) {
            GLFW.glfwSetCursorPos(window, x, y);
            mouseX = x;
            mouseY = y;
            // mouseX = Math.max(0, Math.min(mouseX, Display.getWidth() - 1));
            // mouseY = Math.max(0, Math.min(mouseY, Display.getHeight() - 1));
        }
    }

    public static void update() {}
}
