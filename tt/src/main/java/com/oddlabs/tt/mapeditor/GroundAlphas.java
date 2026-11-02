package com.oddlabs.tt.mapeditor;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.Midpoint;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Works out the ground texture blend maps (the "alphas") from the current heights, the way {@code Landscape}
 * generates them for a new island, so edited ground looks as if the island had been generated that way.
 *
 * <p>The formulas and {@link Channel} operations here follow {@code Landscape.generateAlphas} and the alpha methods
 * it calls, step for step, and must be kept in step with them. They run on a window of the map, with enough margin
 * around the part that is kept for every neighbourhood operation to see the same cells it would on the whole map.
 *
 * <p>Three steps normalize against the whole island: relative height, and the sunlit and shaded ranges of the
 * lighting. Their inputs are cached for the whole map so the island wide ranges stay known. While painting, the
 * ranges the textures were last built with are kept, so only the painted area needs redoing; {@link #rangesMoved}
 * tells when a finished stroke shifted them enough that the whole island should be rebuilt.
 */
final class GroundAlphas {
    // Landscape's private constants.
    private static final float SEABOTTOM_DEPTH_OFFSET_METERS = 3f;
    private static final float UNDERWATER_SHADING = 0.3f;
    /** Largest shift of an island wide range, as a share of that range, that goes unrebuilt. */
    private static final float RANGE_TOLERANCE = 1f / 512f;
    private static final float LIGHT_THRESHOLD = (float) Math.sqrt(0.5f);
    /** Cells beyond a window's kept part needed by the neighbourhood operations (lineart, smooth(3), 2x smooth(1)). */
    private static final int MARGIN = 8;

    /** Number of alpha maps, in the order the blend layers use them. */
    static final int NUM_ALPHAS = 7;

    private final float @NonNull [] @NonNull [] heights;
    private final int size;
    private final Landscape.@NonNull TerrainType terrain;
    private final float height_scale;
    private final float access_threshold;
    private final float build_threshold;
    private final float vegetation_amount;
    private final int rel_radius;
    private final float light_nz;
    private final @NonNull Channel grass_noise;
    // What trees, rocks and iron do to the lighting: highlight is scaled by the first, shadow raised to the second.
    private final float @NonNull [] @NonNull [] supply_highlight;
    private final float @NonNull [] @NonNull [] supply_shadow;
    private final int shadow_size;
    private final @NonNull Channel stamp;

    // Whole map caches of the inputs to the island wide ranges.
    private final float @NonNull [] @NonNull [] rel_intensity;
    private final float @NonNull [] @NonNull [] light;

    // The highest ground, or higher.
    private float max_height;

    // The ranges the current textures were built with.
    private @NonNull Ranges ranges;

    private record Ranges(float rel_min, float rel_max, float highlight_min, float highlight_max, float shadow_min,
                          float shadow_max) {
    }

    GroundAlphas(float @NonNull [] @NonNull [] heights, @NonNull MapSettings settings, @NonNull List<int[]> trees,
            @NonNull List<int[]> palm_trees, @NonNull List<int[]> shaded_supplies) {
        this.heights = heights;
        this.size = heights.length;
        this.terrain = Landscape.TerrainType.values()[settings.terrain()];
        int meters = settings.getMetersPerWorld();
        this.height_scale = settings.getHeightScale();
        this.access_threshold = settings.getAccessThreshold();
        this.build_threshold = access_threshold / 2f;
        this.vegetation_amount = 0.25f + 0.75f * settings.trees() / (float) MapSettings.SLIDER_MAX;
        this.rel_radius = Math.max(1, size >> 5);
        this.light_nz = 2f * HeightMap.METERS_PER_UNIT_GRID / height_scale;
        this.grass_noise = new Midpoint(size, 4, 0.45f, Globals.LANDSCAPE_SEED).toChannel();
        this.supply_highlight = new float[size][size];
        this.supply_shadow = new float[size][size];
        for (float[] row : supply_highlight)
            java.util.Arrays.fill(row, 1f);
        // Landscape.placeSupplies: the size of the soft square a tree darkens around it.
        this.shadow_size = switch (meters) {
            case 256 -> Math.max(size >> 5, 2);
            case 512 -> Math.max(size >> 6, 2);
            case 1024 -> Math.max(size >> 7, 2);
            default -> Math.max(size >> 8, 2);
        };
        this.stamp = new Channel(shadow_size << 1, shadow_size << 1).place(new Channel(shadow_size, shadow_size).fill(
                1f), shadow_size >> 1, shadow_size >> 1).smoothFast();
        stampSupplies(trees, palm_trees, shaded_supplies, 0, 0, size - 1, size - 1);

        this.rel_intensity = new float[size][size];
        this.light = new float[size][size];
        refreshCaches(0, 0, size - 1, size - 1);
        this.ranges = measureRanges();
        measureMaxHeight();
    }

    int getSize() {
        return size;
    }

    /** Cells a tree's shadow reaches on either side of it. */
    int getSupplyShadowReach() {
        return shadow_size;
    }

    /**
     * Redoes what trees, rocks and iron do to the lighting within a rectangle of cells (inclusive), given every
     * resource whose shadow reaches into it.
     */
    void restampSupplies(@NonNull List<int[]> trees, @NonNull List<int[]> palm_trees,
            @NonNull List<int[]> shaded_supplies, int x0, int y0, int x1, int y1) {
        x0 = Math.max(0, x0);
        y0 = Math.max(0, y0);
        x1 = Math.min(size - 1, x1);
        y1 = Math.min(size - 1, y1);
        for (int y = y0; y <= y1; y++) {
            java.util.Arrays.fill(supply_highlight[y], x0, x1 + 1, 1f);
            java.util.Arrays.fill(supply_shadow[y], x0, x1 + 1, 0f);
        }
        stampSupplies(trees, palm_trees, shaded_supplies, x0, y0, x1, y1);
    }

    private void stampSupplies(@NonNull List<int[]> trees, @NonNull List<int[]> palm_trees,
            @NonNull List<int[]> shaded_supplies, int x0, int y0, int x1, int y1) {
        // Landscape.placeSupplies: each tree darkens a soft square around it.
        stampTrees(trees, 0.33f, x0, y0, x1, y1);
        stampTrees(palm_trees, 0.25f, x0, y0, x1, y1);
        // Rocks and iron shade their own cell.
        for (int[] supply : shaded_supplies) {
            if (supply[0] >= x0 && supply[0] <= x1 && supply[1] >= y0 && supply[1] <= y1)
                supply_shadow[supply[1]][supply[0]] = Math.max(supply_shadow[supply[1]][supply[0]], 0.5f);
        }
    }

    /** Stamps tree shadows, wrapping at the map edge as Channel.place does, onto cells within the rectangle. */
    private void stampTrees(@NonNull List<int[]> positions, float strength, int x0, int y0, int x1, int y1) {
        for (int[] tree : positions) {
            int sx = tree[0] - shadow_size + 1;
            int sy = tree[1] - shadow_size + 1;
            for (int y = 0; y < stamp.getHeight(); y++) {
                for (int x = 0; x < stamp.getWidth(); x++) {
                    int mx = Math.floorMod(sx + x, size);
                    int my = Math.floorMod(sy + y, size);
                    if (mx < x0 || mx > x1 || my < y0 || my > y1)
                        continue;
                    float alpha = stamp.getPixel(x, y);
                    supply_highlight[my][mx] *= 1f - alpha;
                    supply_shadow[my][mx] = Math.max(supply_shadow[my][mx], alpha * strength);
                }
            }
        }
    }

    private float normalized(int x, int y) {
        return heights[y][x] / height_scale;
    }

    private float normalizedWrap(int x, int y) {
        return heights[Math.floorMod(y, size)][Math.floorMod(x, size)] / height_scale;
    }

    // ---- Whole map caches and ranges ----

    /** Refreshes the cached inputs affected by height changes in the given rectangle (inclusive). */
    void heightsChanged(int x0, int y0, int x1, int y1) {
        refreshCaches(x0 - rel_radius, y0 - rel_radius, x1 + rel_radius, y1 + rel_radius);
        // Only ever raised here, which can only make cast shadows reach further than needed.
        for (int y = Math.max(0, y0); y <= Math.min(size - 1, y1); y++)
            for (int x = Math.max(0, x0); x <= Math.min(size - 1, x1); x++)
                max_height = Math.max(max_height, heights[y][x]);
    }

    private void measureMaxHeight() {
        max_height = 0f;
        for (float[] row : heights)
            for (float h : row)
                max_height = Math.max(max_height, h);
    }

    private void refreshCaches(int x0, int y0, int x1, int y1) {
        x0 = Math.max(0, x0);
        y0 = Math.max(0, y0);
        x1 = Math.min(size - 1, x1);
        y1 = Math.min(size - 1, y1);
        // Channel.relativeIntensity: height less its box average of radius rel_radius, plus 0.5, with the map
        // wrapping at its edges. The box sums come from a summed area table over the window the boxes cover.
        int wx0 = x0 - rel_radius;
        int wy0 = y0 - rel_radius;
        int w = x1 - x0 + 1 + 2 * rel_radius;
        int h = y1 - y0 + 1 + 2 * rel_radius;
        double[][] sums = new double[h + 1][w + 1];
        for (int y = 0; y < h; y++) {
            double row = 0;
            for (int x = 0; x < w; x++) {
                row += normalizedWrap(wx0 + x, wy0 + y);
                sums[y + 1][x + 1] = sums[y][x + 1] + row;
            }
        }
        int box = 2 * rel_radius + 1;
        float factor = 1f / (box * box);
        for (int y = y0; y <= y1; y++) {
            int by = y - y0;
            for (int x = x0; x <= x1; x++) {
                int bx = x - x0;
                double sum = sums[by + box][bx + box] - sums[by][bx + box] - sums[by + box][bx] + sums[by][bx];
                rel_intensity[y][x] = normalized(x, y) - (float) sum * factor + 0.5f;
            }
        }

        // Sun light from the surface normal, as in Landscape.generateAlphas.
        float lx = 1f / (float) Math.sqrt(2f);
        float nzlz = light_nz * lx;
        float nz2 = light_nz * light_nz;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float nx = normalizedWrap(x + 1, y) - normalizedWrap(x - 1, y);
                float ny = normalizedWrap(x, y + 1) - normalizedWrap(x, y - 1);
                light[y][x] = (nx * lx + nzlz) / (float) Math.sqrt(nx * nx + ny * ny + nz2);
            }
        }
    }

    private @NonNull Ranges measureRanges() {
        float rel_min = Float.POSITIVE_INFINITY;
        float rel_max = Float.NEGATIVE_INFINITY;
        float hl_min = Float.POSITIVE_INFINITY;
        float hl_max = Float.NEGATIVE_INFINITY;
        float sh_min = Float.POSITIVE_INFINITY;
        float sh_max = Float.NEGATIVE_INFINITY;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float rel = rel_intensity[y][x];
                rel_min = Math.min(rel_min, rel);
                rel_max = Math.max(rel_max, rel);
                float highlight = rawHighlight(light[y][x]);
                hl_min = Math.min(hl_min, highlight);
                hl_max = Math.max(hl_max, highlight);
                float shadow = 1f - rawShadow(light[y][x]);
                sh_min = Math.min(sh_min, shadow);
                sh_max = Math.max(sh_max, shadow);
            }
        }
        // Channel.dynamicRangeSymmetric widens the range to be symmetric around 0.5.
        if (rel_min > 1f - rel_max)
            rel_min = 1f - rel_max;
        if (rel_max < 1f - rel_min)
            rel_max = 1f - rel_min;
        return new Ranges(rel_min, rel_max, hl_min, hl_max, sh_min, sh_max);
    }

    /**
     * Whether the island wide ranges moved enough since the textures were last built to show; if so they are taken
     * on and the whole island should be rebuilt.
     */
    boolean rangesMoved() {
        measureMaxHeight();
        Ranges measured = measureRanges();
        boolean moved = moved(ranges.rel_min(), ranges.rel_max(), measured.rel_min(), measured.rel_max())
                || moved(ranges.highlight_min(), ranges.highlight_max(), measured.highlight_min(),
                        measured.highlight_max())
                || moved(ranges.shadow_min(), ranges.shadow_max(), measured.shadow_min(), measured.shadow_max());
        if (moved)
            ranges = measured;
        return moved;
    }

    private static boolean moved(float min, float max, float new_min, float new_max) {
        float range = Math.max(max - min, 1e-6f);
        return Math.abs(new_min - min) / range > RANGE_TOLERANCE || Math.abs(new_max - max) / range > RANGE_TOLERANCE;
    }

    private static float rawHighlight(float light) {
        return light > LIGHT_THRESHOLD ? light : LIGHT_THRESHOLD;
    }

    private static float rawShadow(float light) {
        return light > LIGHT_THRESHOLD ? LIGHT_THRESHOLD : Math.max(0, light);
    }

    // ---- Alphas ----

    /**
     * The rectangle (inclusive) of alphas a height change in the given rectangle can reach, clamped to the map:
     * relative height reaches rel_radius, the smoothing a few cells more, and cast shadows run towards +x for as far
     * as the tallest ground can throw them.
     */
    int @NonNull [] reach(int x0, int y0, int x1, int y1) {
        int spread = rel_radius + MARGIN;
        return new int[]{Math.max(0, x0 - spread), Math.max(0, y0 - spread), Math.min(size - 1,
                x1 + spread + shadowReach()), Math.min(size - 1, y1 + spread)};
    }

    /** How many cells a cast shadow can run: until the falling shadow line drops below any ground. */
    private int shadowReach() {
        return Math.min(size, (int) Math.ceil(max_height / height_scale / shadowDescent()) + 2);
    }

    private float shadowDescent() {
        return 8f / size;
    }

    /**
     * Writes the alphas of a rectangle (inclusive) into two RGBA byte buffers, rows bottom up: the first holds alphas
     * 0 to 3, the second 4 to 6 in red, green and blue.
     */
    void compute(int x0, int y0, int x1, int y1, @NonNull ByteBuffer out_low, @NonNull ByteBuffer out_high) {
        // The window: the kept rectangle plus margin, never past the map edge so edge cells come out as on the
        // whole map, and reaching far enough left for cast shadows coming in from there.
        int wx0 = Math.max(0, x0 - Math.max(MARGIN, shadowReach()));
        int wy0 = Math.max(0, y0 - MARGIN);
        int wx1 = Math.min(size - 1, x1 + MARGIN);
        int wy1 = Math.min(size - 1, y1 + MARGIN);
        int w = wx1 - wx0 + 1;
        int h = wy1 - wy0 + 1;

        Channel height = new Channel(w, h);
        Channel slope = new Channel(w, h);
        Channel relheight = new Channel(w, h);
        Channel noise = new Channel(w, h);
        float rel_factor = 1f / (ranges.rel_max() - ranges.rel_min());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int mx = wx0 + x;
                int my = wy0 + y;
                float center = normalized(mx, my);
                height.putPixel(x, y, center);
                // Channel.lineart, wrapping at the map edge like it does on the whole map.
                slope.putPixel(x, y, Math.max(
                        Math.max(Math.abs(center - normalizedWrap(mx - 1, my)),
                                Math.abs(center - normalizedWrap(mx + 1, my))),
                        Math.max(Math.abs(center - normalizedWrap(mx, my - 1)),
                                Math.abs(center - normalizedWrap(mx, my + 1)))));
                // Channel.relativeIntensityNormalized, with the island wide range.
                relheight.putPixel(x, y, rel_factor * (rel_intensity[my][mx] - ranges.rel_min()));
                noise.putPixel(x, y, grass_noise.getPixel(mx, my));
            }
        }

        Channel[] alphas = new Channel[NUM_ALPHAS];
        switch (terrain) {
            case NATIVE -> {
                alphas[0] = dirtAlpha(height, relheight);
                alphas[1] = rubbleAlpha(height, slope, relheight);
                alphas[2] = slope.copy().threshold(access_threshold, 1f);
                alphas[3] = grassAlpha(height, slope, relheight, noise);
            }
            case VIKING -> {
                alphas[0] = dirtAlpha(height, relheight);
                alphas[1] = slope.copy().threshold(access_threshold, 1f);
                alphas[2] = grassAlpha(height, slope, relheight, noise);
                Channel snow = height.copy().dynamicRange(0.5f, 0.6f, 0f, 1f);
                snow.channelSubtract(alphas[1].copy());
                alphas[3] = snow.smooth(1).smooth(1);
            }
        }
        Channel[] lighting = lighting(height, wx0, wy0);
        alphas[4] = lighting[0];
        alphas[5] = lighting[1];
        float depth_offset = SEABOTTOM_DEPTH_OFFSET_METERS / height_scale;
        alphas[6] = height.copy().invert().dynamicRange(1f - Globals.SEA_LEVEL + depth_offset, 1f, 0f, 1f).gamma(0.5f);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int cx = x - wx0;
                int cy = y - wy0;
                for (int i = 0; i < 4; i++)
                    out_low.put(toByte(alphas[i].getPixel(cx, cy)));
                for (int i = 4; i < 7; i++)
                    out_high.put(toByte(alphas[i].getPixel(cx, cy)));
                out_high.put((byte) 0);
            }
        }
    }

    /** As GLByteImage stores a channel. */
    private static byte toByte(float value) {
        return (byte) Math.clamp(Math.round(value * 255), 0, 255);
    }

    /** Landscape.generateDirtAlpha, and generateSoilAlpha which is the same. */
    private static @NonNull Channel dirtAlpha(@NonNull Channel height, @NonNull Channel relheight) {
        Channel alpha = height.copy().dynamicRange(1.1f * Globals.SEA_LEVEL, 2f * Globals.SEA_LEVEL, 0f, 1f);
        alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
        return alpha;
    }

    /** Landscape.generateRubbleAlpha. */
    private @NonNull Channel rubbleAlpha(@NonNull Channel height, @NonNull Channel slope,
            @NonNull Channel relheight) {
        Channel alpha = slope.copy().dynamicRange(build_threshold, access_threshold, 0f, 1f);
        alpha.channelSubtract(height.copy().invert().dynamicRange(0.8f, 1f, 0f, 1f));
        alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.65f, 0f, 0.5f));
        return alpha;
    }

    /** Landscape.generateGrassAlpha. */
    private @NonNull Channel grassAlpha(@NonNull Channel height, @NonNull Channel slope, @NonNull Channel relheight,
            @NonNull Channel noise) {
        Channel alpha = noise.copy().dynamicRange(1f - vegetation_amount, 1f, 0f, 1f).gamma2();
        alpha.channelBrightest(slope.copy().dynamicRange(0f, access_threshold, 0f, 1f).invert().dynamicRange(
                1f - vegetation_amount, 1f, 0f, 1f).gamma2());
        alpha.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
        alpha.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
        alpha.channelSubtract(slope.copy().threshold(0.75f * access_threshold, 1f).smooth(3));
        alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));
        return alpha;
    }

    /** The highlight and shadow alphas of Landscape.generateAlphas, with the supply shadows it adds later. */
    private @NonNull Channel @NonNull [] lighting(@NonNull Channel height, int wx0, int wy0) {
        int w = height.getWidth();
        int h = height.getHeight();
        Channel highlight = new Channel(w, h);
        Channel shadow = new Channel(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float value = light[wy0 + y][wx0 + x];
                highlight.putPixel(x, y, 0.25f * (rawHighlight(
                        value) - ranges.highlight_min()) / (ranges.highlight_max() - ranges.highlight_min()));
                shadow.putPixel(x, y, 0.75f * (1f - rawShadow(
                        value) - ranges.shadow_min()) / (ranges.shadow_max() - ranges.shadow_min()));
            }
        }

        // Shadows cast along +x, each row starting unshadowed like at the map edge.
        Channel shadowcast = new Channel(w, h);
        float descent = shadowDescent();
        for (int y = 0; y < h; y++) {
            float peak = 0f;
            for (int x = 0; x < w; x++) {
                float value = height.getPixel(x, y);
                peak = peak - descent;
                if (peak > value) {
                    shadowcast.putPixel(x, y, 1f);
                } else {
                    peak = value;
                }
            }
        }
        shadow.channelBrightest(shadowcast.smooth(1).brightness(0.67f));

        float shading_band = SEABOTTOM_DEPTH_OFFSET_METERS / height_scale;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float value = height.getPixel(x, y);
                if (value < Globals.SEA_LEVEL) {
                    float fade = Math.clamp((value - (Globals.SEA_LEVEL - shading_band)) / shading_band, 0f, 1f);
                    float factor = UNDERWATER_SHADING + (1f - UNDERWATER_SHADING) * fade;
                    shadow.putPixel(x, y, shadow.getPixel(x, y) * factor);
                    highlight.putPixel(x, y, highlight.getPixel(x, y) * factor);
                }
                highlight.putPixel(x, y, highlight.getPixel(x, y) * supply_highlight[wy0 + y][wx0 + x]);
                shadow.putPixel(x, y, Math.max(shadow.getPixel(x, y), supply_shadow[wy0 + y][wx0 + x]));
            }
        }
        return new Channel[]{highlight, shadow};
    }
}
