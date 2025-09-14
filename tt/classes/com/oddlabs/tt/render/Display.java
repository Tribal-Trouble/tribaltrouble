package com.oddlabs.tt.render;

import static org.lwjgl.opengl.GL11.*;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.Mouse;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

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

        // Set the window icon after the window is shown and context is created
        setWindowIcon();

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

    /**
     * Sets the window icon using a PNG image from resources.
     * Uses Java's built-in ImageIO for loading and GLFW for setting the icon.
     */
    private final static void setWindowIcon() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Try to load the icon from resources - multiple fallback paths
            InputStream iconStream = null;
            
            if (iconStream == null) {
                iconStream = Display.class.getResourceAsStream("/icon.png");
            }
            
            if (iconStream == null) {
                System.out.println("Window icon not found, skipping icon setup");
                return;
            }

            // Load the image using Java's built-in ImageIO
            BufferedImage image = ImageIO.read(iconStream);
            int width = image.getWidth();
            int height = image.getHeight();

            // Convert BufferedImage to RGBA byte array
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            // Create ByteBuffer for GLFW (RGBA format)
            ByteBuffer buffer = stack.malloc(width * height * 4);
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
            buffer.flip();

            // Create GLFW image structure - try with multiple sizes if possible
            GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1, stack);
            GLFWImage iconImage = iconBuffer.get(0);
            iconImage.set(width, height, buffer);

            GLFW.glfwSetWindowIcon(window, iconBuffer);            
            GLFW.glfwFocusWindow(window);            

        } catch (IOException e) {
            System.out.println("Failed to load window icon: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Failed to set window icon: " + e.getMessage());
        }
    }
}
