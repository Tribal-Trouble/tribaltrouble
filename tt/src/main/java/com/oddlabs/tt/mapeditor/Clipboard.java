package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A copied rectangle of the island: its heights, a row at a time, and its trees, rock and iron as the resource's
 * ordinal and its cell counted from the rectangle's first cell.
 */
record Clipboard(float @NonNull [] @NonNull [] heights, @NonNull List<int @NonNull []> resources) {
    int width() {
        return heights[0].length;
    }

    int height() {
        return heights.length;
    }

    /** The copy turned a quarter turn. */
    @NonNull
    Clipboard rotated() {
        int w = width();
        int h = height();
        float[][] turned = new float[w][h];
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++)
                turned[i][h - 1 - j] = heights[j][i];
        List<int[]> moved = new ArrayList<>(resources.size());
        for (int[] r : resources)
            moved.add(new int[]{r[0], h - 1 - r[2], r[1]});
        return new Clipboard(turned, moved);
    }

    /** The copy mirrored left to right. */
    @NonNull
    Clipboard mirrored() {
        int w = width();
        float[][] flipped = new float[height()][w];
        for (int j = 0; j < height(); j++)
            for (int i = 0; i < w; i++)
                flipped[j][w - 1 - i] = heights[j][i];
        List<int[]> moved = new ArrayList<>(resources.size());
        for (int[] r : resources)
            moved.add(new int[]{r[0], w - 1 - r[1], r[2]});
        return new Clipboard(flipped, moved);
    }
}
