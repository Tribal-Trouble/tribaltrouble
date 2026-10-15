package com.oddlabs.tt.mapeditor;

/**
 * Seeded noise for the brushes: a fixed pattern for a given seed, so a stroke builds on the same shapes for as long as
 * it lasts, and resources painted again land where they did before.
 */
final class Noise {
    private Noise() {
    }

    /** Smoothly interpolated lattice noise in [0, 1]. */
    static float value(float x, float y, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float fx = smooth(x - xi);
        float fy = smooth(y - yi);
        float a = lattice(xi, yi, seed) * (1 - fx) + lattice(xi + 1, yi, seed) * fx;
        float b = lattice(xi, yi + 1, seed) * (1 - fx) + lattice(xi + 1, yi + 1, seed) * fx;
        return a * (1 - fy) + b * fy;
    }

    /**
     * Fractal noise in [0, 1]: octaves of value noise, each half the size and half the weight of the one before, like
     * the generator's midpoint displacement.
     *
     * @param feature size of the largest features, in the units of x and y
     */
    static float fractal(float x, float y, float feature, int octaves, int seed) {
        float sum = 0f;
        float total = 0f;
        float weight = 1f;
        float scale = 1f / feature;
        for (int i = 0; i < octaves; i++) {
            sum += value(x * scale, y * scale, seed + i * 7919) * weight;
            total += weight;
            weight *= .5f;
            scale *= 2f;
        }
        return sum / total;
    }

    /**
     * Ridged fractal noise in [0, 1]: sharp crests where plain noise crosses its middle, the way mountain ridges and
     * the spurs off them look from above.
     */
    static float ridged(float x, float y, float feature, int octaves, int seed) {
        float sum = 0f;
        float total = 0f;
        float weight = 1f;
        float scale = 1f / feature;
        for (int i = 0; i < octaves; i++) {
            float n = 1f - Math.abs(2f * value(x * scale, y * scale, seed + i * 7919) - 1f);
            sum += n * n * weight;
            total += weight;
            weight *= .5f;
            scale *= 2f;
        }
        return sum / total;
    }

    /**
     * Cellular noise, as the generator's Voronoi cliffs: the gap between the distances to the nearest and second
     * nearest of points scattered one to a lattice cell, over the cell size. It is 0 on the borders between cells
     * and rises towards their middles.
     */
    static float cells(float x, float y, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float f1 = Float.POSITIVE_INFINITY;
        float f2 = Float.POSITIVE_INFINITY;
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int cx = xi + ox;
                int cy = yi + oy;
                float px = cx + lattice(cx, cy, seed);
                float py = cy + lattice(cx, cy, seed + 1013);
                float dx = px - x;
                float dy = py - y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
        return Math.min(1f, f2 - f1);
    }

    /** A number in [0, 1] for a cell, the same every time for the same cell and seed. */
    static float hash(int x, int y, int seed) {
        return lattice(x, y, seed);
    }

    static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float lattice(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }
}
