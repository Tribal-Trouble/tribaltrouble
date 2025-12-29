package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class SerializableDisplayMode implements Serializable, Comparable<SerializableDisplayMode> {

    private static final SerializableDisplayMode DEFAULT_MODE = new SerializableDisplayMode(0, 0, 0, 0);

	@Serial
	private static final long serialVersionUID = 1;

    private final int width;
    private final int height;
    private final int freq;
    private final int bpp;



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

    private static int getDistanceFromBestMode(@NonNull SerializableDisplayMode mode) {
        int dx = Math.abs(DEFAULT_MODE.getWidth() - mode.getWidth());
        int dy = Math.abs(DEFAULT_MODE.getHeight() - mode.getHeight());
        return dx + dy;
    }


    public static boolean isModeValid(@NonNull SerializableDisplayMode mode) {
        return mode.getWidth() >= 800 && mode.getHeight() >= 600 && mode.getBitsPerPixel() >= 8 && mode.getFrequency() >= 24;
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