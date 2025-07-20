package com.oddlabs.tt.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;

import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.Mouse;

public final strictfp class Display {
    private static long window;
    private static boolean created = false;
    private static String title = "Tribal Trouble";
    private static int width = 1920;
    private static int height = 1080;

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
        
        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);

        GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, videoMode.refreshRate());
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        width = videoMode.width();
        height = videoMode.height();

        window = GLFW.glfwCreateWindow(width, height, title, monitor, 0);

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        GLFW.glfwShowWindow(window);

        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        
        GL.createCapabilities();

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
        return GLFW.glfwGetClipboardString(window);
    }

    public static boolean isALCreated() {
        return true;
    }
} 
