package com.oddlabs.tt.window;

import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.GLFW_AUTO_ICONIFY;
import static org.lwjgl.glfw.GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.GLFW_ICONIFIED;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwFocusWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorContentScale;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPhysicalSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetVideoModes;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwGetWindowContentScale;
import static org.lwjgl.glfw.GLFW.glfwGetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwIconifyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwRestoreWindow;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public final class LWJGL3Window implements Window {

    private long windowHandle = MemoryUtil.NULL;
    private @NonNull String title = "Tribal Trouble";
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
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) {
        ensureGLFW();

        if (windowHandle != MemoryUtil.NULL) {
            // Reconfigure existing window
            long monitor = fullscreen ? glfwGetPrimaryMonitor() : MemoryUtil.NULL;
            int refreshRate = fullscreen ? mode.getFrequency() : GLFW_DONT_CARE;
            
            if (fullscreen) {
                glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.getWidth(), mode.getHeight(), refreshRate);
            } else {
                // Windowed mode: center on screen
                long currentMonitor = getCurrentMonitor();
                GLFWVidMode vidmode = glfwGetVideoMode(currentMonitor);
                if (vidmode != null) {
                    int x = (vidmode.width() - mode.getWidth()) / 2;
                    int y = (vidmode.height() - mode.getHeight()) / 2;
                    glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, x, y, mode.getWidth(), mode.getHeight(), refreshRate);
                }
            }
            
            if (!fullscreen) {
                glfwSetWindowSize(windowHandle, mode.getWidth(), mode.getHeight());
            }
            
            glfwSetWindowSizeLimits(windowHandle, 800, 600, GLFW_DONT_CARE, GLFW_DONT_CARE);
            
            return;
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
        }
        
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

        glfwSetWindowSizeLimits(windowHandle, 800, 600, GLFW_DONT_CARE, GLFW_DONT_CARE);

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
            Callbacks.glfwFreeCallbacks(windowHandle);
            glfwDestroyWindow(windowHandle);
            windowHandle = MemoryUtil.NULL;
        }
        if (initialized.compareAndSet(true, false)) {
            glfwTerminate();
            Objects.requireNonNull(glfwSetErrorCallback(null)).free();
        }
    }

    public boolean isOpen() {
        return windowHandle != MemoryUtil.NULL;
    }

    @Override
    public void update() {
        glfwSwapBuffers(windowHandle);
        pollEvents();
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public boolean isCloseRequested() {
        return glfwWindowShouldClose(windowHandle);
    }

    @Override
    public void setCloseRequested(boolean value) {
        if (windowHandle != MemoryUtil.NULL) {
            glfwSetWindowShouldClose(windowHandle, value);
        }
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
    public boolean isIconified() {
        return glfwGetWindowAttrib(windowHandle, GLFW_ICONIFIED) == GLFW_TRUE;
    }

    @Override
    public boolean wasResized() {
        boolean r = resized;
        resized = false;
        return r;
    }

    @Override
    public void setIcon(@NonNull Path imagePath) {
        if (windowHandle == MemoryUtil.NULL) return;
        
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            // STBImage requires absolute path or relative to CWD.
            ByteBuffer image = STBImage.stbi_load(imagePath.toString(), w, h, comp, 4);
            if (image == null) {
                System.err.println("Failed to load icon: " + imagePath + " Reason: " + STBImage.stbi_failure_reason());
                return;
            }

            GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);
            icons.position(0);
            icons.width(w.get(0));
            icons.height(h.get(0));
            icons.pixels(image);

            glfwSetWindowIcon(windowHandle, icons);
            
            STBImage.stbi_image_free(image);
        }
    }

    @Override
    public void restore() {
        glfwRestoreWindow(windowHandle);
    }

    @Override
    public void minimize() {
        glfwIconifyWindow(windowHandle);
    }

    @Override
    public void show() {
        glfwShowWindow(windowHandle);
    }

    @Override
    public void focus() {
        glfwFocusWindow(windowHandle);
    }

    @Override
    public int getWidth() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return w[0];
    }

    @Override
    public int getHeight() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return h[0];
    }

    @Override
    public void setTitle(@NonNull String title) {
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
    public @NonNull SerializableDisplayMode @NonNull [] getAvailableDisplayModes() {
        ensureGLFW();
        long monitor = getCurrentMonitor();
        if (monitor == MemoryUtil.NULL) {
            return new SerializableDisplayMode[0];
        }
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
                        mode -> (mode.getWidth() << 16) + mode.getHeight(),
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
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) {
        create(mode, glfwGetWindowMonitor(windowHandle) != MemoryUtil.NULL);
    }

    @Override
    public void makeCurrent() throws Exception {
        glfwMakeContextCurrent(windowHandle);
    }
    
    public long getHandle() {
        return windowHandle;
    }

    @Override
    public boolean isFullscreen() {
        return windowHandle != MemoryUtil.NULL && glfwGetWindowMonitor(windowHandle) != MemoryUtil.NULL;
    }

    private long getCurrentMonitor() {
        if (windowHandle != MemoryUtil.NULL) {
            long monitor = glfwGetWindowMonitor(windowHandle);
            if (monitor != MemoryUtil.NULL) {
                return monitor;
            }
        }
        return glfwGetPrimaryMonitor();
    }

    @Override
    public int[] getMonitorPhysicalSize() {
        ensureGLFW();
        long monitor = getCurrentMonitor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetMonitorPhysicalSize(monitor, w, h);
            return new int[]{w.get(0), h.get(0)};
        }
    }

    @Override
    public float[] getMonitorContentScale() {
        ensureGLFW();
        long monitor = getCurrentMonitor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            glfwGetMonitorContentScale(monitor, x, y);
            return new float[]{x.get(0), y.get(0)};
        }
    }

    @Override
    public float[] getWindowContentScale() {
        if (windowHandle == MemoryUtil.NULL) return new float[]{1.0f, 1.0f};
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            glfwGetWindowContentScale(windowHandle, x, y);
            return new float[]{x.get(0), y.get(0)};
        }
    }
}
