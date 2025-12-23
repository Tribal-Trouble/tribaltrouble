package com.oddlabs.tt.window;

import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetVideoModes;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwGetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public final class LWJGL3Window implements Window {

    private long windowHandle = MemoryUtil.NULL;
    private String title = "Tribal Trouble";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private boolean resized = false;

    public LWJGL3Window() {
    }

    private static void ensureGLFW() {
        if (initialized.compareAndSet(false, true)) {
            GLFWErrorCallback.createPrint(System.err).set();
            if (!glfwInit()) {
                initialized.set(false);
                throw new IllegalStateException("Unable to initialize GLFW");
            }
        }
    }

    @Override
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) throws Exception {
        ensureGLFW();

        if (windowHandle != MemoryUtil.NULL) {
            // Reconfigure existing window
            long monitor = fullscreen ? glfwGetPrimaryMonitor() : MemoryUtil.NULL;
            int refreshRate = fullscreen ? mode.getFrequency() : GLFW_DONT_CARE;
            
            if (fullscreen) {
                glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.getWidth(), mode.getHeight(), refreshRate);
            } else {
                // Windowed mode: center on screen
                GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
                int x = (vidmode.width() - mode.getWidth()) / 2;
                int y = (vidmode.height() - mode.getHeight()) / 2;
                glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, x, y, mode.getWidth(), mode.getHeight(), refreshRate);
            }
            
            if (!fullscreen) {
                glfwSetWindowSize(windowHandle, mode.getWidth(), mode.getHeight());
            }
            
            return;
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        // Request an OpenGL 4.1 Core Profile context
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long monitor = MemoryUtil.NULL;
        if (fullscreen) {
            monitor = glfwGetPrimaryMonitor();
        }

        windowHandle = glfwCreateWindow(mode.getWidth(), mode.getHeight(), title, monitor, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window if not fullscreen
        if (!fullscreen) {
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                        windowHandle,
                        (vidmode.width() - mode.getWidth()) / 2,
                        (vidmode.height() - mode.getHeight()) / 2
                );
            }
        }
        
        // Setup callbacks
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.resized = true;
        });

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
        
        glfwShowWindow(windowHandle);
    }

    @Override
    public void close() {
        if (windowHandle != MemoryUtil.NULL) {
            glfwDestroyWindow(windowHandle);
            windowHandle = MemoryUtil.NULL;
        }
        if (initialized.compareAndSet(true, false)) {
            glfwTerminate();
            Objects.requireNonNull(glfwSetErrorCallback(null)).free();
        }
    }

    @Override
    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    @Override
    public boolean isCloseRequested() {
        return glfwWindowShouldClose(windowHandle);
    }

    @Override
    public boolean isActive() {
        return glfwGetWindowAttrib(windowHandle, GLFW_FOCUSED) == GLFW_TRUE;
    }

    @Override
    public boolean isVisible() {
        return glfwGetWindowAttrib(windowHandle, GLFW_VISIBLE) == GLFW_TRUE;
    }

    @Override
    public boolean wasResized() {
        boolean r = resized;
        resized = false;
        return r;
    }

    @Override
    public int getWidth() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return w[0];
    }

    @Override
    public int getHeight() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return h[0];
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
        if (windowHandle != MemoryUtil.NULL) {
            glfwSetWindowTitle(windowHandle, title);
        }
    }

    @Override
    public void setVSyncEnabled(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    @Override
    public void setFullscreen(boolean fullscreen) throws Exception {
        create(getDisplayMode(), fullscreen);
    }

    @Override
    public @NonNull SerializableDisplayMode[] getAvailableDisplayModes() {
        ensureGLFW();
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode.Buffer modes = glfwGetVideoModes(monitor);
        
        if (modes == null) return new SerializableDisplayMode[0];

        return modes.stream()
                .map(m -> {
                    int bpp = m.redBits() + m.greenBits() + m.blueBits();
                    if (bpp == 24) bpp = 32; // Assume 32-bit if RGB is 24-bit
                    return new SerializableDisplayMode(m.width(), m.height(), bpp, m.refreshRate());
                })
                .filter(SerializableDisplayMode::isModeValid)
                .collect(Collectors.toMap(
                        // Only offer one mode per resolution, the highest bpp and frequency
                        mode -> mode.getWidth() << 16 + mode.getHeight(),
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(SerializableDisplayMode::getBitsPerPixel)
                                .thenComparing(SerializableDisplayMode::getFrequency))
                )).values().stream()
                .sorted(Comparator.reverseOrder())
                .toArray(SerializableDisplayMode[]::new);
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        ensureGLFW();
        
        int width, height;
        
        if (windowHandle != MemoryUtil.NULL) {
            width = getWidth();
            height = getHeight();
        } else {
            // Fallback default
            width = 1280;
            height = 1024;
        }

        long monitor = windowHandle != MemoryUtil.NULL ? glfwGetWindowMonitor(windowHandle) : MemoryUtil.NULL;
        if (monitor == MemoryUtil.NULL) monitor = glfwGetPrimaryMonitor();
        
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        int freq = 60;
        int bpp = 32;
        
        if (vidmode != null) {
            freq = vidmode.refreshRate();
            bpp = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
            if (bpp == 24) bpp = 32;
        }
        
        SerializableDisplayMode current = new SerializableDisplayMode(width, height, bpp, freq);
        
        // Try to match with available modes to ensure exact equality (for UI selection)
        try {
            for (SerializableDisplayMode m : getAvailableDisplayModes()) {
                if (m.getWidth() == width && m.getHeight() == height && m.getBitsPerPixel() == bpp && m.getFrequency() == freq) {
                    return m;
                }
            }
            // Relaxed match: just resolution and BPP
            for (SerializableDisplayMode m : getAvailableDisplayModes()) {
                if (m.getWidth() == width && m.getHeight() == height && m.getBitsPerPixel() == bpp) {
                    return m;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

         try {
             SerializableDisplayMode[] available = getAvailableDisplayModes();
             if (available.length > 0) {
                 return available[0];
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
         return new SerializableDisplayMode(1280, 1024, 32, 60);
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception {
        create(mode, glfwGetWindowMonitor(windowHandle) != MemoryUtil.NULL);
    }

    @Override
    public void makeCurrent() throws Exception {
        glfwMakeContextCurrent(windowHandle);
    }
    
    public long getHandle() {
        return windowHandle;
    }
}
