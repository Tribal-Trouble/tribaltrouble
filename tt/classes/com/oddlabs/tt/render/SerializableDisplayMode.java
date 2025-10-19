package com.oddlabs.tt.render;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.LocalInput;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SerializableDisplayMode implements Serializable {

    private final static long serialVersionUID = 1;

    private final int width;
    private final int height;
    private final int freq;
    private final int bpp;

    public SerializableDisplayMode(DisplayMode mode) {
        this(mode.getWidth(), mode.getHeight(), mode.getBitsPerPixel(), mode.getFrequency());
    }

    public SerializableDisplayMode(int width, int height, int bpp, int freq) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
    }

    public static void setModeToNearest(SerializableDisplayMode target_mode) throws LWJGLException {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        SortedSet<DisplayMode> set = new TreeSet<>(new DisplayModeComparator(target_mode));
        for (DisplayMode mode : modes) {
            if (isModeValid(mode)) {
                System.out.println("modes[i] = " + mode);
                set.add(mode);
            }
        }

        System.out.println("target_mode = " + target_mode);
        if (set.isEmpty())
            throw new LWJGLException("No modes available");
        DisplayMode nearest_mode = (DisplayMode) set.first();
        LWJGLException last_exception = new LWJGLException("No suitable mode found");
        DisplayMode mode = null;
        while (!set.isEmpty()) {
            mode = (DisplayMode) set.first();
            set.remove(mode);
            // Only consider modes with the same size as the nearest mode to avoid too many tries
            if (mode.getHeight() != nearest_mode.getHeight() || mode.getWidth() != nearest_mode.getWidth())
                continue;
            try {
                System.out.println("considering mode = " + mode);
                nativeSetMode(mode);
                createWindow();
                GL11.glViewport(0, 0, mode.getWidth(), mode.getHeight());
                if (!Display.getDisplayMode().equals(mode))
                    throw new RuntimeException(Display.getDisplayMode() + " does not match " + mode);
                last_exception = null;
                break;
            } catch (LWJGLException e) {
                mode = null;
                last_exception = e;
                System.out.println(mode + " failed because of " + e.getMessage());
            }
        }
        last_exception = deterministic.log(last_exception);
        if (last_exception != null)
            throw last_exception;
    }

    private static void createWindow() throws LWJGLException {
        int[] depth_array = new int[]{24, 16};
        int[] samples_array = new int[]{/*Settings.getSettings().samples, */0};
        LWJGLException last_exception = new LWJGLException("Could not find a suitable pixel format");
        for (int d = 0; d < depth_array.length; d++) {
            for (int s = 0; s < samples_array.length; s++) {
                int depth = depth_array[d];
                int samples = samples_array[s];
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

    public static boolean isModeValid(DisplayMode mode) {
        return mode.getWidth() >= 800 && mode.getHeight() >= 600;
    }

    public static void switchMode(SerializableDisplayMode mode) {
        try {
            doSetMode(mode);
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFullscreen(boolean fullscreen, boolean switch_now) {
        LocalInput.getLocalInput().fullscreenToggled(fullscreen, switch_now);
    }

    private static void nativeSetMode(DisplayMode mode) throws LWJGLException {
        if (!Display.getDisplayMode().equals(mode)) {
            System.out.println("setting mode = " + mode);
            Display.setDisplayMode(mode);
            Renderer.resetInput();
        }
    }

    private static void doSetMode(SerializableDisplayMode target_mode) throws LWJGLException {
        DisplayMode[] lwjgl_modes = Display.getAvailableDisplayModes();
        for (DisplayMode lwjgl_mode : lwjgl_modes) {
            SerializableDisplayMode mode = new SerializableDisplayMode(lwjgl_mode);
            if (mode.equals(target_mode)) {
                nativeSetMode(lwjgl_mode);
                GL11.glViewport(0, 0, lwjgl_mode.getWidth(), lwjgl_mode.getHeight());
                return;
            }
        }
        throw new LWJGLException("Could not find mode matching: " + target_mode);
    }

    @Override
    public String toString() {
        return width + "x" + height + " " + bpp + "bit " + freq + "Hz";
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrequency() {
        return freq;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SerializableDisplayMode))
            return false;
        SerializableDisplayMode other_mode = (SerializableDisplayMode) other;
        return isEquivalent(other_mode) && getFrequency() == other_mode.getFrequency() && getBitsPerPixel() == other_mode.getBitsPerPixel();
    }

    public boolean isEquivalent(SerializableDisplayMode other_mode) {
        return getWidth() == other_mode.getWidth() && getHeight() == other_mode.getHeight();
    }

    @Override
    public int hashCode() {
        return width ^ height ^ freq ^ bpp;
    }
}
