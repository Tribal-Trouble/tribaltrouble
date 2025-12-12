package com.oddlabs.tt.window;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import com.oddlabs.tt.util.GLUtils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LWJGL2Window implements Window {

    @Override
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) throws Exception {
        setModeToNearest(mode);
        if (fullscreen) {
            Display.setFullscreen(true);
        }
    }

    private void setModeToNearest(@NonNull SerializableDisplayMode target_mode) throws LWJGLException {
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        // Assuming DisplayModeComparator logic needs to be replicated or moved.
        // For now, simpler selection or move comparator here.
        // Legacy used DisplayModeComparator. I will implement simple logic here.
        
        DisplayMode best = null;
        for (DisplayMode mode : modes) {
            if (mode.getWidth() == target_mode.getWidth() && mode.getHeight() == target_mode.getHeight()) {
                if (best == null || mode.getBitsPerPixel() > best.getBitsPerPixel() || (mode.getBitsPerPixel() == best.getBitsPerPixel() && mode.getFrequency() > best.getFrequency())) {
                    best = mode;
                }
            }
        }
        
        if (best == null) {
             // Fallback to current
             best = Display.getDisplayMode();
        }

        try {
            System.out.println("considering mode = " + best);
            nativeSetMode(best);
            createWindow();
            GL11.glViewport(0, 0, best.getWidth(), best.getHeight());
        } catch (LWJGLException e) {
            throw e;
        }
    }

    private void nativeSetMode(@NonNull DisplayMode mode) throws LWJGLException {
        if (!Display.getDisplayMode().equals(mode)) {
            System.out.println("setting mode = " + mode);
            Display.setDisplayMode(mode);
            Renderer.resetInput();
        }
    }

    private void createWindow() throws LWJGLException {
        if (Display.isCreated()) return;
        
        int[] depth_array = new int[]{24, 16};
        int[] samples_array = new int[]{8, 4, 2, 0};
        LWJGLException last_exception = new LWJGLException("Could not find a suitable pixel format");
        for (int j : depth_array) {
            for (int i : samples_array) {
                int depth = j;
                int samples = i;
                try {
                    Display.create(new PixelFormat(0, depth, 0, samples));
                    return;
                } catch (LWJGLException e) {
                    last_exception = e;
                    System.out.println("Failed window: depthbits = " + depth + " | samples = " + samples + " with exception " + e);
                }
            }
        }
        throw last_exception;
    }

    @Override
    public void destroy() {
        Display.destroy();
    }

    @Override
    public void update() {
        Display.update();
    }

    @Override
    public boolean isCloseRequested() {
        return Display.isCloseRequested();
    }

    @Override
    public boolean isActive() {
        return Display.isActive();
    }

    @Override
    public boolean isVisible() {
        return Display.isVisible();
    }

    @Override
    public boolean wasResized() {
        return Display.wasResized();
    }

    @Override
    public int getWidth() {
        return Display.getWidth();
    }

    @Override
    public int getHeight() {
        return Display.getHeight();
    }

    @Override
    public void setTitle(String title) {
        Display.setTitle(title);
    }

    @Override
    public void setVSyncEnabled(boolean enabled) {
        Display.setVSyncEnabled(enabled);
    }

    @Override
    public void setFullscreen(boolean fullscreen) throws Exception {
        Display.setFullscreen(fullscreen);
    }

    @Override
    public @NonNull SerializableDisplayMode[] getAvailableDisplayModes() throws Exception {
        try {
            return Arrays.stream(Display.getAvailableDisplayModes())
                    .filter(m -> m.getWidth() >= 800 && m.getHeight() >= 600 && m.getBitsPerPixel() >= 8 && m.getFrequency() >= 24)
                    .collect(Collectors.toMap(
                            // Only offer one mode per resolution, the highest bpp and frequency
                            mode -> mode.getWidth() << 16 + mode.getHeight(),
                            Function.identity(),
                            BinaryOperator.maxBy(Comparator.comparing(DisplayMode::getBitsPerPixel)
                                    .thenComparing(DisplayMode::getFrequency))
                    )).values().stream()
                    .map(SerializableDisplayMode::new)
                    .sorted()
                    .toArray(SerializableDisplayMode[]::new);
        } catch (LWJGLException e) {
            throw new IllegalStateException("Could not get available modes", e);
        }
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        return new SerializableDisplayMode(Display.getDisplayMode());
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception {
        // Convert SerializableDisplayMode to LWJGL DisplayMode
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        for (DisplayMode m : modes) {
            if (m.getWidth() == mode.getWidth() && m.getHeight() == mode.getHeight() && 
                m.getBitsPerPixel() == mode.getBitsPerPixel() && m.getFrequency() == mode.getFrequency()) {
                nativeSetMode(m);
                GL11.glViewport(0, 0, m.getWidth(), m.getHeight());
                return;
            }
        }
        throw new LWJGLException("Mode not found: " + mode);
    }

    @Override
    public void makeCurrent() throws Exception {
        Display.makeCurrent();
    }
}
