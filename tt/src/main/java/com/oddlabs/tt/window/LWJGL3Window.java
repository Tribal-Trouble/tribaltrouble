package com.oddlabs.tt.window;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.system.macosx.ObjCRuntime;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.GLFW_AUTO_ICONIFY;
import static org.lwjgl.glfw.GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.GLFW_ICONIFIED;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
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
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwIconifyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwRestoreWindow;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowAttrib;
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

public final class LWJGL3Window implements Window {
    private static final Logger logger = Logger.getLogger(LWJGL3Window.class.getSimpleName());
    private static final String os = System.getProperty("os.name").toLowerCase();
    private static final boolean isMac = os.contains("mac");

    // When true, fullscreen uses borderless windowed mode (undecorated window at native monitor
    // resolution) on all platforms instead of exclusive fullscreen. This is the Steam-friendly path
    // (overlay, alt-tab, screen capture all behave). Set false to use bondolo's exclusive fullscreen
    // on Windows/Linux. Mac is always borderless regardless of this flag.
    private static final boolean BORDERLESS_FULLSCREEN = true;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static long objc_msgSend = MemoryUtil.NULL;
    private static long nsAppClass = MemoryUtil.NULL;
    private static long sharedApplicationSel = MemoryUtil.NULL;
    private static long isActiveSel = MemoryUtil.NULL;
    private static long setPresentationOptionsSel = MemoryUtil.NULL;
    private static long nsApp = MemoryUtil.NULL;
    private static boolean macInitialized = false;

    private static void initMacFFI() {
        if (isMac && !macInitialized) {
            try {
                objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                nsAppClass = ObjCRuntime.objc_getClass("NSApplication");
                sharedApplicationSel = ObjCRuntime.sel_getUid("sharedApplication");
                isActiveSel = ObjCRuntime.sel_getUid("isActive");
                setPresentationOptionsSel = ObjCRuntime.sel_getUid("setPresentationOptions:");
                nsApp = JNI.invokePPP(nsAppClass, sharedApplicationSel, objc_msgSend);
                macInitialized = true;
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to initialize macOS FFI caching", t);
            }
        }
    }

    private boolean isMacAppActive() {
        if (isMac) {
            initMacFFI();
            if (macInitialized && nsApp != MemoryUtil.NULL) {
                try {
                    return JNI.invokePPZ(nsApp, isActiveSel, objc_msgSend);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Failed to call [NSApp isActive]", t);
                }
            }
        }
        return false;
    }

    private long windowHandle = MemoryUtil.NULL;
    private @NonNull String title = "Tribal Trouble";
    private boolean resized;
    private boolean closeRequested;
    private boolean isFullscreen;

    public LWJGL3Window() {
        ensureGLFW();
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
        this.isFullscreen = fullscreen;

        if (windowHandle != MemoryUtil.NULL) {
            // Reconfigure existing window
            float scale = getMonitorContentScale().x;
            int logicalW = (int) (mode.getWidth() / scale);
            int logicalH = (int) (mode.getHeight() / scale);
            logger.log(Level.INFO, "Reconfiguring window: " + mode + " (logical: " + logicalW + "x" + logicalH
                    + "), fullscreen: " + fullscreen + ", scale: " + scale);

            long monitor = (fullscreen && !isMac && !BORDERLESS_FULLSCREEN) ? glfwGetPrimaryMonitor() : MemoryUtil.NULL;
            int refreshRate = fullscreen ? mode.getFrequency() : GLFW_DONT_CARE;

            if (fullscreen) {
                if (isMac || BORDERLESS_FULLSCREEN) {
                    glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_FALSE);
                    long currentMonitor = getCurrentMonitor();
                    GLFWVidMode vidmode = glfwGetVideoMode(currentMonitor);
                    if (vidmode != null) {
                        glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, 0, 0, vidmode.width(), vidmode.height(),
                                GLFW_DONT_CARE);
                    }
                    setMacPresentationOptions(10);
                } else {
                    glfwSetWindowMonitor(windowHandle, monitor, 0, 0, logicalW, logicalH, refreshRate);
                }
            } else {
                if (isMac) {
                    glfwSetWindowAttrib(windowHandle, GLFW_DECORATED, GLFW_TRUE);
                    setMacPresentationOptions(0);
                }
                // Windowed mode: center on screen
                long currentMonitor = getCurrentMonitor();
                GLFWVidMode vidmode = glfwGetVideoMode(currentMonitor);
                if (vidmode != null) {
                    int x = (vidmode.width() - logicalW) / 2;
                    int y = (vidmode.height() - logicalH) / 2;
                    glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, x, y, logicalW, logicalH,
                            refreshRate);
                }

                // After exiting fullscreen, the content scale might have changed (especially on macOS Retina).
                // Re-calculate logical size with the new scale to avoid 2x sized windows.
                float newScale = getMonitorContentScale().x;
                if (newScale != scale) {
                    logger.log(Level.INFO, "Scale changed from " + scale + " to " + newScale
                            + " after exiting fullscreen. Adjusting window size.");
                    scale = newScale;
                    logicalW = (int) (mode.getWidth() / scale);
                    logicalH = (int) (mode.getHeight() / scale);
                    glfwSetWindowSize(windowHandle, logicalW, logicalH);

                    // Re-center
                    vidmode = glfwGetVideoMode(currentMonitor);
                    if (vidmode != null) {
                        glfwSetWindowPos(windowHandle, (vidmode.width() - logicalW) / 2, (vidmode.height() - logicalH)
                                / 2);
                    }
                }
            }

            if (!fullscreen) {
                // Ensure the window size is exactly what we want, in case glfwSetWindowMonitor didn't do it perfectly
                glfwSetWindowSize(windowHandle, logicalW, logicalH);
            }

            glfwSetWindowSizeLimits(windowHandle, (int) (SerializableDisplayMode.MIN_WIDTH / scale),
                    (int) (SerializableDisplayMode.MIN_HEIGHT / scale),
                    GLFW_DONT_CARE, GLFW_DONT_CARE);

            glfwFocusWindow(windowHandle);
            syncViewport();
            int[] actualW = new int[1];
            int[] actualH = new int[1];
            glfwGetWindowSize(windowHandle, actualW, actualH);
            logger.log(Level.INFO, "Reconfiguration complete. Logical window size: " + actualW[0] + "x" + actualH[0]
                    + ", Framebuffer size: " + getWidth() + "x" + getHeight());
            return;
        }

        float scale = getMonitorContentScale().x;
        int logicalW = (int) (mode.getWidth() / scale);
        int logicalH = (int) (mode.getHeight() / scale);
        logger.log(Level.INFO, "Creating window: " + mode + " (logical: " + logicalW + "x" + logicalH
                + "), fullscreen: " + fullscreen + ", scale: " + scale);

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);

        if (isMac) {
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
        }
        if (fullscreen && (isMac || BORDERLESS_FULLSCREEN)) {
            glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                logicalW = vidmode.width();
                logicalH = vidmode.height();
            }
        }

        Settings settings = Renderer.getRenderer().getSettings();
        if (settings.view_samples > 0) {
            glfwWindowHint(GLFW_SAMPLES, settings.view_samples);
        }

        // Request an OpenGL 4.1 Core Profile context
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long monitor = MemoryUtil.NULL;
        if (fullscreen && !isMac && !BORDERLESS_FULLSCREEN) {
            monitor = glfwGetPrimaryMonitor();
        }

        windowHandle = glfwCreateWindow(logicalW, logicalH, title, monitor, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the GLFW window");
        }

        glfwSetWindowSizeLimits(windowHandle, (int) (SerializableDisplayMode.MIN_WIDTH / scale),
                (int) (SerializableDisplayMode.MIN_HEIGHT / scale),
                GLFW_DONT_CARE, GLFW_DONT_CARE);

        // Center the window if not fullscreen, or position at (0,0) if borderless fullscreen on Mac
        if (fullscreen && (isMac || BORDERLESS_FULLSCREEN)) {
            glfwSetWindowPos(windowHandle, 0, 0);
            setMacPresentationOptions(10);
        } else if (!fullscreen) {
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                        windowHandle,
                        (vidmode.width() - logicalW) / 2,
                        (vidmode.height() - logicalH) / 2
                );
            }
        }

        // Setup callbacks
        glfwSetFramebufferSizeCallback(windowHandle, (_, fw, fh) -> {
            this.resized = true;
            Renderer.getRenderer().getRenderContext().setViewport(0, 0, fw, fh);
        });
        glfwSetWindowCloseCallback(windowHandle, (_) -> {
            setCloseRequested(true);
            glfwSetWindowShouldClose(windowHandle, false); // Cancel the actual close immediately
        });

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        // Initial viewport sync
        syncViewport();

        glfwShowWindow(windowHandle);
        glfwFocusWindow(windowHandle);
    }

    private void syncViewport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fw = stack.mallocInt(1);
            IntBuffer fh = stack.mallocInt(1);
            glfwGetFramebufferSize(windowHandle, fw, fh);
            Renderer.getRenderer().getRenderContext().setViewport(0, 0, fw.get(0), fh.get(0));
        }
    }

    @Override
    public void close() {
        if (isMac) {
            setMacPresentationOptions(0);
        }
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

    @Override
    public boolean isOpen() {
        return windowHandle != MemoryUtil.NULL;
    }

    @Override
    public void update() {
        glfwSwapBuffers(windowHandle);
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    public void pollEvents(int timeoutMs) {
        if (timeoutMs > 0) {
            GLFW.glfwWaitEventsTimeout(timeoutMs / 1000.0);
        } else {
            glfwPollEvents();
        }
    }

    @Override
    public boolean isCloseRequested() {
        boolean r = closeRequested;
        setCloseRequested(false);
        return r;
    }

    @Override
    public void setCloseRequested(boolean value) {
        closeRequested = value;
    }

    @Override
    public boolean isActive() {
        if (isMac) {
            return isMacAppActive() || glfwGetWindowAttrib(windowHandle, GLFW_FOCUSED) == GLFW_TRUE;
        }
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

        if (isMac) {
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

    /**
     * Returns the physical framebuffer width in pixels.
     * Uses {@code glfwGetFramebufferSize}.
     */
    @Override
    public int getWidth() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return w[0];
    }

    /**
     * Returns the physical framebuffer height in pixels.
     * Uses {@code glfwGetFramebufferSize}.
     */
    @Override
    public int getHeight() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        return h[0];
    }

    /**
     * Returns the logical window width in screen coordinates.
     * Uses {@code glfwGetWindowSize}.
     */
    @Override
    public int getLogicalWidth() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(windowHandle, w, h);
        return w[0];
    }

    /**
     * Returns the logical window height in screen coordinates.
     * Uses {@code glfwGetWindowSize}.
     */
    @Override
    public int getLogicalHeight() {
        assert windowHandle != MemoryUtil.NULL;
        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(windowHandle, w, h);
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
    public @NonNull List<@NonNull SerializableDisplayMode> getFullscreenDisplayModes() {
        return getAvailableDisplayModes();
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getWindowedDisplayModes() {
        int[][] standardResolutions = {
                {1024, 768}, {1280, 720}, {1280, 800}, {1280, 1024},
                {1440, 900}, {1600, 900}, {1600, 1200}, {1680, 1050},
                {1920, 1080}, {1920, 1200}, {2560, 1440}, {2560, 1600},
                {3440, 1440}, {3840, 2160}
        };

        long monitor = getCurrentMonitor();
        int maxW = Integer.MAX_VALUE;
        int maxH = Integer.MAX_VALUE;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetMonitorWorkarea(monitor, x, y, w, h);
            maxW = w.get(0);
            maxH = h.get(0);
        }

        final int finalMaxW = maxW;
        final int finalMaxH = maxH;
        var current = getDisplayMode();
        float scale = getMonitorContentScale().x;

        var modes = Arrays.stream(standardResolutions)
                .filter(res -> (res[0] / scale) <= finalMaxW && (res[1] / scale) <= finalMaxH)
                .map(res -> new SerializableDisplayMode(res[0], res[1], current.getBitsPerPixel(), current
                        .getFrequency()))
                .filter(SerializableDisplayMode::isModeValid)
                .collect(Collectors.toCollection(ArrayList::new));

        // Ensure current mode is included if it fits
        if (modes.stream().noneMatch(current::isEquivalent)) {
            if ((current.getWidth() / scale) <= finalMaxW && (current.getHeight() / scale) <= finalMaxH) {
                modes.add(current);
            }
        }

        return modes.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getAvailableDisplayModes() {
        long monitor = getCurrentMonitor();
        float scale = getMonitorContentScale().x;

        return Optional.ofNullable(monitor != MemoryUtil.NULL ? glfwGetVideoModes(monitor) : null).stream()
                .flatMap(StructBuffer::stream)
                .map(m -> {
                    int bpp = m.redBits() + m.greenBits() + m.blueBits();
                    if (bpp == 24) bpp = 32;
                    // Convert to physical pixels
                    return new SerializableDisplayMode((int) (m.width() * scale), (int) (m.height() * scale), bpp,
                            m.refreshRate());
                })
                .filter(SerializableDisplayMode::isModeValid)
                .collect(Collectors.toMap(
                        // Only offer one mode per resolution, the highest bpp and frequency
                        mode -> (mode.getWidth() << 16) + mode.getHeight(),
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparingInt(SerializableDisplayMode::getBitsPerPixel)
                                .thenComparingInt(SerializableDisplayMode::getFrequency))
                )).values().stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        int width, height;

        if (windowHandle != MemoryUtil.NULL) {
            width = getWidth(); // Physical Pixels
            height = getHeight();
        } else {
            // Fallback default
            width = SerializableDisplayMode.MIN_WIDTH;
            height = SerializableDisplayMode.MIN_HEIGHT;
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

            if (windowHandle == MemoryUtil.NULL) {
                // Return physical pixels
                float scale = getMonitorContentScale().x;
                width = (int) (vidmode.width() * scale);
                height = (int) (vidmode.height() * scale);
            }
        }

        return new SerializableDisplayMode(width, height, bpp, freq);
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) {
        create(mode, isFullscreen());
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
        return isFullscreen;
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
    public @NonNull Vector2f getMonitorPhysicalSize() {
        long monitor = getCurrentMonitor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetMonitorPhysicalSize(monitor, w, h);
            return new Vector2f(w.get(0), h.get(0));
        }
    }

    @Override
    public @NonNull Vector2f getMonitorContentScale() {
        long monitor = getCurrentMonitor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            glfwGetMonitorContentScale(monitor, x, y);
            return new Vector2f(x.get(0), y.get(0));
        }
    }

    @Override
    public @NonNull Vector2f getWindowContentScale() {
        if (windowHandle == MemoryUtil.NULL) return new Vector2f(1.0f, 1.0f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            glfwGetWindowContentScale(windowHandle, x, y);
            return new Vector2f(x.get(0), y.get(0));
        }
    }

    @Override
    public float getPixelDensity() {
        return getWindowContentScale().x;
    }

    private void setMacPresentationOptions(int options) {
        if (isMac) {
            try {
                initMacFFI();
                if (macInitialized && nsApp != MemoryUtil.NULL) {
                    JNI.invokePPPV(nsApp, setPresentationOptionsSel, (long) options, objc_msgSend);
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to set macOS presentation options", t);
            }
        }
    }
}
