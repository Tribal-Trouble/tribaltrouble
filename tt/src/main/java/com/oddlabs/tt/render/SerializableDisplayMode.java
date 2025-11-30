package com.oddlabs.tt.render;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.LocalInput;
import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SerializableDisplayMode implements Serializable, Comparable<SerializableDisplayMode> {

    private static final SerializableDisplayMode DEFAULT_MODE = new SerializableDisplayMode(0, 0, 0, 0);

	@Serial
	private static final long serialVersionUID = 1;

    private final int width;
    private final int height;
    private final int freq;
    private final int bpp;

    public SerializableDisplayMode(@NonNull DisplayMode mode) {
        this(mode.getWidth(), mode.getHeight(), mode.getBitsPerPixel(), mode.getFrequency());
    }

    public SerializableDisplayMode(int width, int height, int bpp, int freq) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
    }

    @Override
    public int compareTo(@NonNull SerializableDisplayMode o) {
        /*
         * Elias: sort after largest bpp first, then lowest freq
         * to accommodate broken monitors lying about their
         * capabilities
         */
        int freq_dist1 = Math.abs(this.getFrequency() - DEFAULT_MODE.getFrequency());
        int freq_dist2 = Math.abs(o.getFrequency() - DEFAULT_MODE.getFrequency());
        int bpp_dist1 = Math.abs(this.getBitsPerPixel() - DEFAULT_MODE.getBitsPerPixel());
        int bpp_dist2 = Math.abs(this.getBitsPerPixel() - DEFAULT_MODE.getBitsPerPixel());
        return getDistanceFromBestMode(this) < getDistanceFromBestMode(o)
                ? -1
                : getDistanceFromBestMode(this) > getDistanceFromBestMode(o)
                    ? 1
                    : bpp_dist1 < bpp_dist2
                        ? -1
                        : bpp_dist1 > bpp_dist2
                            ? 1
                            : Integer.compare(freq_dist1, freq_dist2);
    }

    private int getDistanceFromBestMode(@NonNull SerializableDisplayMode mode) {
        int dx = Math.abs(DEFAULT_MODE.getWidth() - mode.getWidth());
        int dy = Math.abs(DEFAULT_MODE.getHeight() - mode.getHeight());
        return dx + dy;
    }

    public static void setModeToNearest(@NonNull SerializableDisplayMode target_mode) throws LWJGLException {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        SortedSet<DisplayMode> set = new TreeSet<>(new DisplayModeComparator(target_mode));
        Arrays.stream(modes)
                .filter(SerializableDisplayMode::isModeValid)
                .forEach(set::add);

		IO.println("display modes : " + set.size() + " target_mode = " + target_mode);
        if (set.isEmpty())
            throw new LWJGLException("No modes available");
        DisplayMode nearest_mode = set.getFirst();
        LWJGLException last_exception = new LWJGLException("No suitable mode found");
        DisplayMode mode = null;
        while (!set.isEmpty()) {
            mode = set.getFirst();
            set.remove(mode);
            // Only consider modes with the same size as the nearest mode to avoid too many tries
            if (mode.getHeight() != nearest_mode.getHeight() || mode.getWidth() != nearest_mode.getWidth())
                continue;
            try {
				IO.println("considering mode = " + mode);
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
				IO.println(mode + " failed because of " + e.getMessage());
            }
        }
        last_exception = deterministic.log(last_exception);
        if (last_exception != null)
            throw last_exception;
    }

    private static void createWindow() throws LWJGLException {
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
					IO.println("Failed window: depthbits = " + depth + " | samples = " + samples + " with exception " + e);
                }
            }
        }
        throw last_exception;
    }

    public static @NonNull SerializableDisplayMode @NonNull [] getAvailableModes() {
        try {
            return Arrays.stream(Display.getAvailableDisplayModes())
                    .filter(SerializableDisplayMode::isModeValid)
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

    public static boolean isModeValid(@NonNull DisplayMode mode) {
        return mode.getWidth() >= 800 && mode.getHeight() >= 600 && mode.getBitsPerPixel() >= 8 && mode.getFrequency() >= 24;
    }

    public static void switchMode(@NonNull SerializableDisplayMode mode) {
        try {
            doSetMode(mode);
        } catch (LWJGLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void setFullscreen(boolean fullscreen, boolean switch_now) {
        LocalInput.getLocalInput().fullscreenToggled(fullscreen, switch_now);
    }

    private static void nativeSetMode(@NonNull DisplayMode mode) throws LWJGLException {
        if (!Display.getDisplayMode().equals(mode)) {
			IO.println("setting mode = " + mode);
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
    public @NonNull String toString() {
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
        return other instanceof SerializableDisplayMode other_mode &&
                isEquivalent(other_mode) && getFrequency() == other_mode.getFrequency() &&
                getBitsPerPixel() == other_mode.getBitsPerPixel();
    }

    public boolean isEquivalent(@NonNull SerializableDisplayMode other_mode) {
        return getWidth() == other_mode.getWidth() && getHeight() == other_mode.getHeight();
    }

    @Override
    public int hashCode() {
        return width ^ height ^ freq ^ bpp;
    }
}
