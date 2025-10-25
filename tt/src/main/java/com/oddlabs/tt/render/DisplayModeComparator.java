package com.oddlabs.tt.render;

import org.lwjgl.opengl.DisplayMode;

import java.util.Comparator;

public final class DisplayModeComparator implements Comparator<DisplayMode> {

    private final SerializableDisplayMode target_mode;

    public DisplayModeComparator(SerializableDisplayMode target_mode) {
        this.target_mode = target_mode;
    }

    @Override
    public int compare(DisplayMode d1, DisplayMode d2) {
        /*
		 * Elias: sort after largest bpp first, then lowest freq
		 * to accomodate broken monitors lying about their
		 * capabilities
         */
        int freq_dist1 = Math.abs(d1.getFrequency() - target_mode.getFrequency());
        int freq_dist2 = Math.abs(d2.getFrequency() - target_mode.getFrequency());
        int bpp_dist1 = Math.abs(d1.getBitsPerPixel() - target_mode.getBitsPerPixel());
        int bpp_dist2 = Math.abs(d2.getBitsPerPixel() - target_mode.getBitsPerPixel());
        if (getDistanceFromBestMode(d1) < getDistanceFromBestMode(d2))
            return -1;
        else
            if (getDistanceFromBestMode(d1) > getDistanceFromBestMode(d2))
                return 1;
            else
                if (bpp_dist1 < bpp_dist2)
                    return -1;
                else
                    if (bpp_dist1 > bpp_dist2)
                        return 1;
                    else
                        return Integer.compare(freq_dist1, freq_dist2);
    }

    private int getDistanceFromBestMode(DisplayMode mode) {
        int dx = Math.abs(target_mode.getWidth() - mode.getWidth());
        int dy = Math.abs(target_mode.getHeight() - mode.getHeight());
        return dx + dy;
    }
}
