package com.oddlabs.tt.render;

import static org.lwjgl.opengl.GL11.*;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.Mouse;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

public final strictfp class Display {
    private static long window;
    private static boolean created = false;
    private static String title = "Tribal Trouble";
    private static int width = 1920;
    private static int height = 1080;
    private static int refreshRate = 60;
    private static boolean fullscreen = false;
    private static long monitor;

    public static boolean isCreated() {
        return created;
    }

    public static void create() {
        if (created) {
            return;
        }

        if (!GLFW.glfwInit()) {
            System.out.println("Unable to initialize GLFW");
            return;
        }

        System.out.println("Is settings null? " + (Settings.getSettings() == null));
        width = Settings.getSettings().view_width;
        height = Settings.getSettings().view_height;
        fullscreen = Settings.getSettings().fullscreen;
        refreshRate = Settings.getSettings().view_freq;

        monitor = GLFW.glfwGetPrimaryMonitor();

        System.out.println("width: " + width);
        System.out.println("height: " + height);
        System.out.println("fullscreen: " + fullscreen);
        System.out.println("refreshRate: " + refreshRate);
        GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, refreshRate);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        /* GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);*/

        if (fullscreen) {
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            window = GLFW.glfwCreateWindow(width, height, title, monitor, 0);
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            window = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        }

        // Initalize the cursor settings with what was saved - autoload of the settings won't call
        // the setter
        Settings.getSettings().setNativeCursor(Settings.getSettings().getNativeCursor());

        GLFW.glfwShowWindow(window);

        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Simple debug to check actual dimensions
        try {
            int[] ww = new int[1];
            int[] wh = new int[1];
            GLFW.glfwGetWindowSize(window, ww, wh);
            // System.out.println("ACTUAL WINDOW SIZE: " + ww[0] + "x" + wh[0]);
            width = ww[0];
            height = wh[0];
        } catch (Exception e) {
            System.out.println("Could not get window size: " + e.getMessage());
        }

        created = true;

        Mouse.create();
        Keyboard.create();
    }

    public static void destroy() {
        if (created) {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
            created = false;
        }
    }

    public static void update() {
        if (created) {
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    public static boolean shouldClose() {
        return created && GLFW.glfwWindowShouldClose(window);
    }

    public static boolean isCloseRequested() {
        return shouldClose();
    }

    public static void makeCurrent() {
        if (created) {
            GLFW.glfwMakeContextCurrent(window);
        }
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static long getWindow() {
        return window;
    }

    public static boolean isDirty() {
        return true;
    }

    public static String getClipboard() {
        return ""; // glfwGetClipboardString(window);
    }

    public static boolean isALCreated() {
        return true;
    }

    public static GLFWVidMode[] getVidModes() {
        long target_monitor = GLFW.glfwGetPrimaryMonitor();
        org.lwjgl.glfw.GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(target_monitor);
        if (buffer == null) {
            return new GLFWVidMode[0];
        }
        GLFWVidMode[] modes = new GLFWVidMode[buffer.remaining()];
        for (int i = 0; i < buffer.remaining(); i++) {
            modes[i] = buffer.get(i);
        }
        return modes;
    }
}
