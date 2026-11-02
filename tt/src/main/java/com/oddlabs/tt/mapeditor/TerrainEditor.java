package com.oddlabs.tt.mapeditor;

import com.oddlabs.procedural.Tools;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Applies brushes to the island's height map and keeps the renderer in step.
 *
 * <p>The editor writes straight into the height array the {@link HeightMap} was built from, then uploads the
 * changed rectangle to the height texture in one go. Patch bounds, which culling and picking rely on, are grown
 * through {@link HeightMap#editHeight} but only for patches whose bounds the edit actually exceeded.
 *
 * <p>All coordinates here are grid units (one grid unit is {@link HeightMap#METERS_PER_UNIT_GRID} meters).
 */
final class TerrainEditor {
    static final float MIN_HEIGHT = 0f;
    static final float MAX_HEIGHT = 150f;

    /** Meters per second a full intensity height brush raises the ground at its center. */
    private static final float RAISE_RATE = 30f;
    /** How fast (1/seconds) smooth closes the gap to the local average at full intensity. */
    private static final float SETTLE_RATE = 8f;
    /** Largest fraction of the gap one frame of smooth may close, keeping sharpen stable. */
    private static final float MAX_SETTLE_STEP = 0.5f;
    /**
     * How fast (1/seconds) flatten closes the gap to its target at half intensity. The rate climbs steeply
     * towards full intensity, where flatten snaps to the target in one go.
     */
    private static final float FLATTEN_RATE = 15f;
    /** Share of a flatten brush's or a ramp's width that is fully flat, the rest blends into the ground around. */
    private static final float PLATEAU_CORE = 0.6f;
    static final int MAX_UNDO_STEPS = 32;

    /** Meters an islet's shore stands above the sea, enough to walk on. */
    private static final float SHORE_RISE = 1f;
    /** Meters the middle of an islet rises above its shore at full intensity. */
    private static final float ISLET_CROWN = 8f;
    /** Meters below the sea a strait, or a river at no intensity, is dug to. */
    private static final float SHALLOW_DEPTH = 1.5f;
    /** Further meters a strait or a river is dug at full intensity. */
    private static final float EXTRA_DEPTH = 6f;
    /** Height in meters of a ridge's crest at no intensity, and how much higher it gets at full. */
    private static final float RIDGE_MIN = 4f;
    private static final float RIDGE_RANGE = 56f;
    /** Meters per second full intensity roughness adds to the bumps' height. */
    private static final float ROUGHEN_RATE = 12f;
    /** Grid units per second a full intensity warp moves the ground at the brush's middle. */
    private static final float WARP_RATE = 6f;
    /** Radians per second a full intensity twist turns the middle of the brush, and a swirl its very middle. */
    private static final float TWIST_RATE = 1.5f;
    private static final float SWIRL_RATE = 4f;
    /** Share of a twist's width that turns as one piece, the rest shearing into the ground around. */
    private static final float TWIST_CORE = .5f;
    /** Share of a stretch's width that holds together at full intensity. */
    private static final float STRETCH_CORE = .7f;
    /** Meters ground the stretch drags rises or sinks for each grid unit it ends up from where it lay: a half slope. */
    private static final float STRETCH_SKEW = HeightMap.METERS_PER_UNIT_GRID * .5f;
    /** Steps one stretch is cut into at most, however far the cursor went since the last. */
    private static final int MAX_STRETCH_STEPS = 256;
    /** How fast (1/seconds) erosion and beaches close the gap at full intensity. */
    private static final float ERODE_RATE = 20f;
    /** Share of the steepest step units walk that a path climbs at most per grid unit. */
    private static final float PATH_GRADE = .7f;
    /** Sweeps that ease a path's bends at most; a winding path settles in a few. */
    private static final int MAX_EASING_PASSES = 64;
    /** The four neighbours of a cell, x and y in turn. */
    private static final int[] NEIGHBOURS = {0, 1, -1, 0, 1, 0, 0, -1};

    private final @NonNull HeightMap height_map;
    private final float @NonNull [] @NonNull [] heights;
    private final int size;
    private final float sea_level;
    // The largest height step, in meters, between neighbouring cells units can still walk.
    private final float walk_step;
    private final @NonNull FloatBuffer upload;
    private final float @NonNull [] @NonNull [] scratch;
    private final Deque<@NonNull UndoStep> undo_steps = new ArrayDeque<>();
    // The heights undo took back, newest first, until the next stroke.
    private final Deque<@NonNull UndoStep> redo_steps = new ArrayDeque<>();

    // Rectangle (inclusive) changed since the last flush.
    private int dirty_x0 = Integer.MAX_VALUE;
    private int dirty_y0 = Integer.MAX_VALUE;
    private int dirty_x1 = Integer.MIN_VALUE;
    private int dirty_y1 = Integer.MIN_VALUE;

    // Heights at the start of the current stroke, and the rectangle the stroke has touched.
    private float @Nullable [] @Nullable [] stroke_backup;
    private int stroke_x0;
    private int stroke_y0;
    private int stroke_x1;
    private int stroke_y1;

    private boolean modified;

    // For the brushes that move the ground about: where the ground now in each cell lay when the stroke began, in
    // grid units, and whether that holds for the stroke in progress. Made the first time one of them is used.
    private float @Nullable [] @Nullable [] origin_x;
    private float @Nullable [] @Nullable [] origin_y;
    private float @Nullable [] @Nullable [] scratch_y;
    private boolean origins_ready;

    private record UndoStep(int x0, int y0, float @NonNull [] @NonNull [] heights) {
    }

    /** Told about each rectangle of grid cells (inclusive) whose heights reached the renderer. */
    @FunctionalInterface
    interface ChangeListener {
        void heightsChanged(int x0, int y0, int x1, int y1);
    }

    private final @NonNull ChangeListener listener;

    /**
     * @param heights the array the height map was built from; the height map keeps and reads the same array
     */
    TerrainEditor(@NonNull HeightMap height_map, float @NonNull [] @NonNull [] heights, @NonNull MapSettings settings,
            @NonNull ChangeListener listener) {
        this.height_map = height_map;
        this.listener = listener;
        this.heights = heights;
        this.size = heights.length;
        this.sea_level = height_map.getSeaLevelMeters();
        // AccessMap: a cell is walkable when no neighbour is more than the threshold away, in height map units.
        this.walk_step = settings.getAccessThreshold() * settings.getHeightScale();
        this.upload = BufferUtils.createFloatBuffer(size * size);
        this.scratch = new float[size][size];
    }

    int getSize() {
        return size;
    }

    boolean isModified() {
        return modified;
    }

    void markSaved() {
        modified = false;
    }

    /** The heights themselves, a row at a time, for a shared session to keep in step; see {@link #applyShared}. */
    float @NonNull [] @NonNull [] heights() {
        return heights;
    }

    float @NonNull [] @NonNull [] copyHeights() {
        float[][] copy = new float[size][];
        for (int y = 0; y < size; y++)
            copy[y] = heights[y].clone();
        return copy;
    }

    /** Height at a grid position, bilinearly interpolated. */
    float getHeight(float gx, float gy) {
        return sample(heights, gx, gy);
    }

    /** A value of a grid as large as the map at a grid position, bilinearly interpolated. */
    private float sample(float @NonNull [] @NonNull [] grid, float gx, float gy) {
        gx = Math.clamp(gx, 0f, size - 1);
        gy = Math.clamp(gy, 0f, size - 1);
        int x0 = Math.min((int) gx, size - 2);
        int y0 = Math.min((int) gy, size - 2);
        float fx = gx - x0;
        float fy = gy - y0;
        float h0 = grid[y0][x0] * (1 - fx) + grid[y0][x0 + 1] * fx;
        float h1 = grid[y0 + 1][x0] * (1 - fx) + grid[y0 + 1][x0 + 1] * fx;
        return h0 * (1 - fy) + h1 * fy;
    }

    // ---- Strokes and undo ----

    void beginStroke() {
        if (stroke_backup != null)
            endStroke();
        stroke_backup = copyHeights();
        origins_ready = false;
        stroke_x0 = Integer.MAX_VALUE;
        stroke_y0 = Integer.MAX_VALUE;
        stroke_x1 = Integer.MIN_VALUE;
        stroke_y1 = Integer.MIN_VALUE;
    }

    /** @return whether the stroke changed anything, and so left an undo step */
    boolean endStroke() {
        float[][] backup = stroke_backup;
        stroke_backup = null;
        if (backup == null || stroke_x0 > stroke_x1)
            return false;
        // Only the touched rectangle of the backup is worth keeping.
        int w = stroke_x1 - stroke_x0 + 1;
        int h = stroke_y1 - stroke_y0 + 1;
        float[][] saved = new float[h][w];
        for (int y = 0; y < h; y++)
            System.arraycopy(backup[stroke_y0 + y], stroke_x0, saved[y], 0, w);
        undo_steps.push(new UndoStep(stroke_x0, stroke_y0, saved));
        while (undo_steps.size() > MAX_UNDO_STEPS)
            undo_steps.removeLast();
        redo_steps.clear();
        return true;
    }

    /** Restores the heights from before the last stroke. */
    boolean undo() {
        endStroke();
        return swap(undo_steps, redo_steps);
    }

    /** Lays again the heights the last undo took back. */
    boolean redo() {
        endStroke();
        return swap(redo_steps, undo_steps);
    }

    /** Forgets what undo took back, as an edit made since leaves nothing to redo. */
    void clearRedo() {
        redo_steps.clear();
    }

    /** Lays the newest step of one stack, keeping the heights it replaced on the other. */
    private boolean swap(@NonNull Deque<@NonNull UndoStep> from, @NonNull Deque<@NonNull UndoStep> to) {
        UndoStep step = from.poll();
        if (step == null)
            return false;
        int w = step.heights()[0].length;
        int h = step.heights().length;
        float[][] replaced = new float[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(heights[step.y0() + y], step.x0(), replaced[y], 0, w);
            System.arraycopy(step.heights()[y], 0, heights[step.y0() + y], step.x0(), w);
        }
        to.push(new UndoStep(step.x0(), step.y0(), replaced));
        markDirty(step.x0(), step.y0(), step.x0() + w - 1, step.y0() + h - 1);
        modified = true;
        flush();
        return true;
    }

    // ---- Shared sessions ----

    /**
     * Lays heights another player's edit brought, a row at a time, leaving a cell alone where its value is NaN. They
     * go into what undo and redo restore as well, so taking back an edit of this player's leaves theirs standing. They
     * are
     * not this player's edit, so neither mark the map modified nor join the stroke in progress.
     */
    void applyShared(int x0, int y0, int width, int height, float @NonNull [] values) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float v = values[j * width + i];
                if (!Float.isNaN(v))
                    heights[y0 + j][x0 + i] = v;
            }
        }
        float[][] backup = stroke_backup;
        if (backup != null)
            copyShared(backup, 0, 0, x0, y0, width, height, values);
        for (UndoStep step : undo_steps)
            copyShared(step.heights(), step.x0(), step.y0(), x0, y0, width, height, values);
        for (UndoStep step : redo_steps)
            copyShared(step.heights(), step.x0(), step.y0(), x0, y0, width, height, values);
        markShown(x0, y0, x0 + width - 1, y0 + height - 1);
    }

    /** Copies shared heights into a saved rectangle of heights whose first cell is (sx0, sy0). */
    private static void copyShared(float @NonNull [] @NonNull [] saved, int sx0, int sy0, int x0, int y0, int width,
            int height, float @NonNull [] values) {
        int ix0 = Math.max(x0, sx0);
        int iy0 = Math.max(y0, sy0);
        int ix1 = Math.min(x0 + width, sx0 + saved[0].length) - 1;
        int iy1 = Math.min(y0 + height, sy0 + saved.length) - 1;
        for (int y = iy0; y <= iy1; y++) {
            for (int x = ix0; x <= ix1; x++) {
                float v = values[(y - y0) * width + x - x0];
                if (!Float.isNaN(v))
                    saved[y - sy0][x - sx0] = v;
            }
        }
    }

    /** Heights in a rectangle (inclusive) changed outside a brush, as by rounding; the renderer should follow. */
    void heightsWritten(int x0, int y0, int x1, int y1) {
        markShown(x0, y0, x1, y1);
    }

    // ---- Brushes ----

    /** Raises (sign 1) or lowers (sign -1) the ground under the brush. */
    void applyHeight(float cx, float cy, float radius, float intensity, int sign, float dt) {
        float amount = sign * intensity * RAISE_RATE * dt;
        forEachCell(cx, cy, radius, (x, y, weight) -> heights[y][x] + amount * weight);
    }

    /**
     * Fills ground below the target height up to it (sign 1), or cuts ground above it down to it (sign -1). At full
     * intensity the middle of the brush lands on the target at once; at half it gets there in a fraction of a second.
     */
    void applyFlatten(float cx, float cy, float radius, float intensity, int sign, float dt, float target) {
        float blend = intensity >= 1f ? 1f : 1f - (float) Math.exp(-FLATTEN_RATE * intensity / (1f - intensity) * dt);
        forEachCell(cx, cy, radius, PLATEAU_CORE, (x, y, weight) -> {
            float h = heights[y][x];
            float gap = target - h;
            if (gap * sign <= 0f)
                return h;
            return h + gap * blend * weight;
        });
    }

    /** Smooths the ground toward its local average (sign 1), or sharpens it away from it (sign -1). */
    void applySmooth(float cx, float cy, float radius, float intensity, int sign, float dt) {
        int x0 = Math.max(0, (int) Math.floor(cx - radius) - 1);
        int y0 = Math.max(0, (int) Math.floor(cy - radius) - 1);
        int x1 = Math.min(size - 1, (int) Math.ceil(cx + radius) + 1);
        int y1 = Math.min(size - 1, (int) Math.ceil(cy + radius) + 1);
        // Average from a copy so the result does not depend on the order cells are visited in.
        for (int y = y0; y <= y1; y++)
            System.arraycopy(heights[y], x0, scratch[y], x0, x1 - x0 + 1);
        float rate = intensity * SETTLE_RATE * dt;
        forEachCell(cx, cy, radius, (x, y, weight) -> {
            float h = scratch[y][x];
            float average = localAverage(x, y);
            return h + sign * (average - h) * Math.min(rate * weight, MAX_SETTLE_STEP);
        });
    }

    /**
     * Heaps up random ground (sign 1) or digs it away (sign -1): knolls, hollows and ridges of many sizes, bent and
     * turned every which way, mostly rising on one side and mostly sinking on the other. Each stroke draws its own
     * feature size, twist and mix of rounded and ridged shapes, and how hard the ground moves wanders over the brush,
     * so some spots rise sharply while others barely stir. The pattern holds for the length of a stroke, so holding
     * the button builds on it.
     */
    void applyRandom(float cx, float cy, float radius, float intensity, int sign, float dt, int seed) {
        float amount = sign * intensity * RAISE_RATE * dt;
        float feature = Math.max(2f, radius * (.15f + .45f * Noise.hash(1, 0, seed)));
        double angle = 2 * Math.PI * Noise.hash(2, 0, seed);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float warp = feature * (.4f + Noise.hash(3, 0, seed));
        // Some strokes are all rounded, some all ridged, the rest a blend.
        float ridged = Math.clamp(2f * Noise.hash(4, 0, seed) - .5f, 0f, 1f);
        forEachCell(cx, cy, radius, (x, y, weight) -> {
            // Turned and bent, so the shapes neither line up with the grid nor come out round.
            float u = x * cos - y * sin;
            float v = x * sin + y * cos;
            float wu = u + warp * (2f * Noise.value(u / feature, v / feature, seed + 11) - 1f);
            float wv = v + warp * (2f * Noise.value(u / feature, v / feature, seed + 23) - 1f);
            // Fractal noise stays near its middle, so it is stretched to reach from hollows to tops.
            float rounded = ridged < 1f ? Math.clamp(.5f + 2f * (Noise.fractal(wu, wv, feature, 4, seed) - .5f), 0f,
                    1f) : 0f;
            float crests = ridged > 0f ? Noise.ridged(wu, wv, feature, 4, seed + 37) : 0f;
            float shape = 1.5f * (rounded + (crests - rounded) * ridged) - .4f;
            float strength = Noise.value(u / (feature * 3f), v / (feature * 3f), seed + 53);
            return heights[y][x] + amount * weight * shape * (.15f + 1.7f * strength * strength);
        });
    }

    /**
     * Lays a straight ramp from (ax, ay) at height ha to (bx, by) at height hb, as wide as the brush. Building
     * (sign 1) only raises ground up to the ramp, carving (sign -1) only lowers it; intensity blends toward it.
     */
    void applyRamp(float ax, float ay, float ha, float bx, float by, float hb, float radius, float intensity,
            int sign) {
        float dx = bx - ax;
        float dy = by - ay;
        float length_sq = dx * dx + dy * dy;
        int x0 = Math.max(0, (int) Math.floor(Math.min(ax, bx) - radius));
        int y0 = Math.max(0, (int) Math.floor(Math.min(ay, by) - radius));
        int x1 = Math.min(size - 1, (int) Math.ceil(Math.max(ax, bx) + radius));
        int y1 = Math.min(size - 1, (int) Math.ceil(Math.max(ay, by) + radius));
        float core = radius * PLATEAU_CORE;
        boolean changed = false;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float t = length_sq > 0f ? Math.clamp(((x - ax) * dx + (y - ay) * dy) / length_sq, 0f, 1f) : 0f;
                float px = ax + dx * t - x;
                float py = ay + dy * t - y;
                float distance = (float) Math.sqrt(px * px + py * py);
                if (distance >= radius)
                    continue;
                float weight = plateau(distance, radius, core);
                float target = ha + (hb - ha) * t;
                float h = heights[y][x];
                float gap = target - h;
                if (gap * sign <= 0f)
                    continue;
                heights[y][x] = clampHeight(x, y, h + gap * weight * intensity);
                changed = true;
            }
        }
        if (changed)
            markDirty(x0, y0, x1, y1);
    }

    // ---- Paths ----

    /** Meters a path may climb per grid unit, short of the steepest step units walk, to leave room for bends. */
    float maxPathGrade() {
        return walk_step * PATH_GRADE;
    }

    /** The height a path takes at a point clicked on ground of the given height: on it, but never in the sea. */
    float pathHeight(float ground) {
        return Math.max(ground, sea_level + SHORE_RISE);
    }

    /**
     * Which legs of a path, from one clicked point to the next, climb too steeply to walk.
     *
     * @param point_heights the path's height at each clicked point
     */
    boolean @NonNull [] steepLegs(@NonNull BrushPath path, float @NonNull [] point_heights) {
        float[] knots = path.knots();
        boolean[] steep = new boolean[Math.max(0, knots.length - 1)];
        for (int i = 0; i < steep.length; i++)
            steep[i] = Math.abs(
                    point_heights[i + 1] - point_heights[i]) > maxPathGrade() * (knots[i + 1] - knots[i]) + 1e-3f;
        return steep;
    }

    /**
     * Lays a walkable path along a course, as wide as the brush. It runs level from side to side and climbs evenly
     * from each clicked point's height to the next, filling ground below it and cutting ground above it, and its
     * sides slope off into the ground around. Tight bends are eased until no step across the path is too high to
     * walk. A leg that climbs too steeply, see {@link #steepLegs}, is eased the same way, so it cannot meet the
     * ground at both ends.
     *
     * @param point_heights the path's height at each clicked point, see {@link #pathHeight}
     * @param intensity     how far beyond the path its sides slope off, from barely to another brush radius
     */
    void applyPath(@NonNull BrushPath path, float @NonNull [] point_heights, float radius, float intensity) {
        float bank = 1f + intensity * radius;
        float reach = radius + bank;
        float[] curve = path.curve();
        float min_x = Float.POSITIVE_INFINITY, min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY, max_y = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < curve.length; i += 2) {
            min_x = Math.min(min_x, curve[i]);
            max_x = Math.max(max_x, curve[i]);
            min_y = Math.min(min_y, curve[i + 1]);
            max_y = Math.max(max_y, curve[i + 1]);
        }
        int x0 = Math.max(0, (int) Math.floor(min_x - reach));
        int y0 = Math.max(0, (int) Math.floor(min_y - reach));
        int x1 = Math.min(size - 1, (int) Math.ceil(max_x + reach));
        int y1 = Math.min(size - 1, (int) Math.ceil(max_y + reach));
        if (x0 > x1 || y0 > y1)
            return;
        int w = x1 - x0 + 1;
        int h = y1 - y0 + 1;
        // The path's height where it runs, NaN off it, and how far each cell is from its middle.
        float[] target = new float[w * h];
        float[] distance = new float[w * h];
        Arrays.fill(target, Float.NaN);
        Arrays.fill(distance, Float.POSITIVE_INFINITY);
        float[] knots = path.knots();
        path.forEachCellWithin(reach, size, (x, y, d, along) -> {
            if (x < x0 || x > x1 || y < y0 || y > y1)
                return;
            int k = (y - y0) * w + (x - x0);
            distance[k] = d;
            if (d <= radius)
                target[k] = profile(knots, point_heights, along);
        });
        keepWalkable(target, w, h, maxPathGrade());
        spreadToSides(target, distance, w, h, reach);
        boolean changed = false;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int k = (y - y0) * w + (x - x0);
                float d = distance[k];
                if (d >= reach || Float.isNaN(target[k]))
                    continue;
                float weight = d <= radius ? 1f : falloff(d - radius, bank);
                float ground = heights[y][x];
                heights[y][x] = clampHeight(x, y, ground + (target[k] - ground) * weight);
                changed = true;
            }
        }
        if (changed)
            markDirty(x0, y0, x1, y1);
    }

    /** A path's height a distance along it: from each clicked point's height evenly to the next. */
    private static float profile(float @NonNull [] knots, float @NonNull [] point_heights, float along) {
        for (int i = 0; i + 1 < knots.length; i++) {
            if (along <= knots[i + 1] || i + 2 == knots.length) {
                float run = knots[i + 1] - knots[i];
                float t = run > 0f ? Math.clamp((along - knots[i]) / run, 0f, 1f) : 1f;
                return point_heights[i] + (point_heights[i + 1] - point_heights[i]) * t;
            }
        }
        return point_heights[0];
    }

    /**
     * Eases the heights of a w by h patch, leaving NaN cells alone, until no cell is more than a step from any
     * neighbour: the mean of the highest such heights nowhere above the patch's and the lowest nowhere below. Where
     * the patch already keeps to the step, as a path does along its legs, both are the patch itself and nothing moves.
     * Where it does not, as across the inside of a tight bend where the nearest leg changes, the jump spreads out.
     */
    private static void keepWalkable(float @NonNull [] target, int w, int h, float step) {
        float[] upper = target.clone();
        float[] lower = target.clone();
        // Each pass sweeps forwards then backwards, which settles a straight path at once and a winding one soon.
        boolean changed = true;
        for (int pass = 0; changed && pass < MAX_EASING_PASSES; pass++) {
            changed = false;
            for (int k = 0; k < target.length; k++) {
                int x = k % w;
                if (x > 0)
                    changed |= ease(upper, lower, k, k - 1, step);
                if (k >= w)
                    changed |= ease(upper, lower, k, k - w, step);
            }
            for (int k = target.length - 1; k >= 0; k--) {
                int x = k % w;
                if (x < w - 1)
                    changed |= ease(upper, lower, k, k + 1, step);
                if (k + w < target.length)
                    changed |= ease(upper, lower, k, k + w, step);
            }
        }
        for (int k = 0; k < target.length; k++)
            target[k] = (upper[k] + lower[k]) / 2f;
    }

    /** Brings a cell within a step of a neighbour, from above in upper and from below in lower. */
    private static boolean ease(float @NonNull [] upper, float @NonNull [] lower, int k, int n, float step) {
        if (Float.isNaN(upper[k]) || Float.isNaN(upper[n]))
            return false;
        boolean changed = false;
        if (upper[n] + step < upper[k]) {
            upper[k] = upper[n] + step;
            changed = true;
        }
        if (lower[n] - step > lower[k]) {
            lower[k] = lower[n] - step;
            changed = true;
        }
        return changed;
    }

    /** Gives each cell beside the path, within reach, the height of the nearest cell of the path. */
    private static void spreadToSides(float @NonNull [] target, float @NonNull [] distance, int w, int h,
            float reach) {
        int[] queue = new int[w * h];
        int tail = 0;
        for (int k = 0; k < target.length; k++)
            if (!Float.isNaN(target[k]))
                queue[tail++] = k;
        for (int head = 0; head < tail; head++) {
            int k = queue[head];
            int x = k % w;
            int y = k / w;
            for (int i = 0; i < NEIGHBOURS.length; i += 2) {
                int nx = x + NEIGHBOURS[i];
                int ny = y + NEIGHBOURS[i + 1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h)
                    continue;
                int n = ny * w + nx;
                if (Float.isNaN(target[n]) && distance[n] < reach) {
                    target[n] = target[k];
                    queue[tail++] = n;
                }
            }
        }
    }

    // ---- Coasts and courses ----

    /**
     * Raises a small island just above the sea (sign 1), or sinks the ground into a strait (sign -1), around a point
     * or along the line from one point to another: an isthmus between two shores, or a channel through land. The
     * coast wanders with noise. Intensity sets how high the island's middle rises above its shore, or how deep the
     * strait is dug.
     */
    void applyIsthmus(float ax, float ay, float bx, float by, float radius, float intensity, int sign, int seed) {
        BrushPath path = new BrushPath(List.of(new float[]{ax, ay}, new float[]{bx, by}));
        float feature = Math.max(3f, radius / 2f);
        float floor = sea_level - SHALLOW_DEPTH - intensity * EXTRA_DEPTH;
        float crown = intensity * ISLET_CROWN;
        forEachNear(path, radius * 1.2f, (x, y, distance, _) -> {
            float h = heights[y][x];
            float t = distance / (radius * (.8f + .4f * Noise.fractal(x, y, feature, 3, seed)));
            if (t >= 1f)
                return h;
            if (sign < 0)
                return h - Math.max(0f, h - floor) * plateau(t, 1f, PLATEAU_CORE);
            // A crown in the middle, a level shore, then a shelf down under the sea.
            float target = t < .75f ? sea_level + SHORE_RISE + crown * Noise.smooth(
                    1f - t / .75f) : sea_level + SHORE_RISE - (SHORE_RISE + SHALLOW_DEPTH) * Noise.smooth(
                            (t - .75f) / .25f);
            return h + Math.max(0f, target - h) * plateau(t, 1f, .85f);
        });
    }

    /**
     * Digs a river along a course, its bed below the sea so the sea fills it, its banks blending into the ground on
     * either side. The width wanders a little along the way; intensity sets how deep it is.
     */
    void applyRiver(@NonNull BrushPath path, float radius, float intensity, int seed) {
        float bed = sea_level - SHALLOW_DEPTH - intensity * EXTRA_DEPTH;
        float wander = Math.max(4f, radius * 2f);
        forEachNear(path, radius * 1.15f, (x, y, distance, along) -> {
            float h = heights[y][x];
            float t = distance / (radius * (.85f + .3f * Noise.value(along / wander, .5f, seed)));
            if (t >= 1f)
                return h;
            return h - Math.max(0f, h - bed) * plateau(t, 1f, .4f);
        });
    }

    /**
     * Raises a mountain ridge along a course: a sharp crest that rises to peaks and dips to saddles along the way,
     * with spurs and gullies running down its flanks. Intensity sets how high the crest is.
     */
    void applyRidge(@NonNull BrushPath path, float radius, float intensity, int seed) {
        float crest = RIDGE_MIN + intensity * RIDGE_RANGE;
        float along_feature = Math.max(4f, radius * 2f);
        float spur_feature = Math.max(3f, radius / 2f);
        forEachNear(path, radius * 1.2f, (x, y, distance, along) -> {
            float h = heights[y][x];
            float t = distance / (radius * (.8f + .4f * Noise.value(along / along_feature, .5f, seed + 1)));
            if (t >= 1f)
                return h;
            // Fractal noise stays near its middle, so it is stretched to reach from saddles to peaks.
            float rise = Math.clamp(.5f + 2.5f * (Noise.fractal(along, .5f, along_feature, 3, seed) - .5f), 0f, 1f);
            float peak = crest * (.45f + .55f * rise);
            float spurs = .3f + Noise.ridged(x, y, spur_feature, 3, seed + 2);
            // Spurs only shape the flanks, so the crest line stays whole.
            float flank = 1f + (spurs - 1f) * Noise.smooth(Math.min(1f, t * 1.5f));
            return h + peak * (float) Math.pow(1f - t, 1.5f) * flank;
        });
    }

    @FunctionalInterface
    private interface PathFunction {
        float apply(int x, int y, float distance, float along);
    }

    private void forEachNear(@NonNull BrushPath path, float reach, @NonNull PathFunction function) {
        int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        path.forEachCellWithin(reach, size, (x, y, distance, along) -> {
            heights[y][x] = clampHeight(x, y, function.apply(x, y, distance, along));
            bounds[0] = Math.min(bounds[0], x);
            bounds[1] = Math.min(bounds[1], y);
            bounds[2] = Math.max(bounds[2], x);
            bounds[3] = Math.max(bounds[3], y);
        });
        if (bounds[0] <= bounds[2])
            markDirty(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    // ---- Texture of the ground ----

    /**
     * Roughens the ground with bumps and dents of every size up to a third of the brush (sign 1), or calms it
     * (sign -1), taking out bumps that small while leaving the larger lie of the land.
     */
    void applyRoughness(float cx, float cy, float radius, float intensity, int sign, float dt, int seed) {
        if (sign > 0) {
            float amount = intensity * ROUGHEN_RATE * dt;
            float feature = Math.max(3f, radius / 3f);
            forEachCell(cx, cy, radius, (x, y, weight) -> heights[y][x] + amount * weight * (2f * Noise.fractal(x, y,
                    feature, 4, seed) - 1f));
            return;
        }
        blurToScratch(cx, cy, radius, Math.max(1, Math.round(radius / 6f)));
        float rate = intensity * SETTLE_RATE * dt;
        forEachCell(cx, cy, radius, (x, y, weight) -> {
            float h = heights[y][x];
            return h + (scratch[y][x] - h) * Math.min(rate * weight, MAX_SETTLE_STEP);
        });
    }

    /**
     * Fills scratch over the brush's square with the heights averaged over a square reaching the given number of
     * cells each way: rows first, then columns, each with running sums so a wide reach costs no more.
     */
    private void blurToScratch(float cx, float cy, float radius, int reach) {
        int x0 = Math.max(0, (int) Math.floor(cx - radius));
        int y0 = Math.max(0, (int) Math.floor(cy - radius));
        int x1 = Math.min(size - 1, (int) Math.ceil(cx + radius));
        int y1 = Math.min(size - 1, (int) Math.ceil(cy + radius));
        if (x0 > x1 || y0 > y1)
            return;
        int sx0 = Math.max(0, x0 - reach);
        int sx1 = Math.min(size - 1, x1 + reach);
        int sy0 = Math.max(0, y0 - reach);
        int sy1 = Math.min(size - 1, y1 + reach);
        float[] sums = new float[Math.max(sx1 - sx0, sy1 - sy0) + 2];
        // Rows, for every row the columns will read.
        for (int y = sy0; y <= sy1; y++) {
            float[] row = heights[y];
            for (int x = sx0; x <= sx1; x++)
                sums[x - sx0 + 1] = sums[x - sx0] + row[x];
            for (int x = x0; x <= x1; x++) {
                int a = Math.max(sx0, x - reach);
                int b = Math.min(sx1, x + reach);
                scratch[y][x] = (sums[b - sx0 + 1] - sums[a - sx0]) / (b - a + 1);
            }
        }
        float[] column = new float[sy1 - sy0 + 1];
        for (int x = x0; x <= x1; x++) {
            for (int y = sy0; y <= sy1; y++)
                sums[y - sy0 + 1] = sums[y - sy0] + scratch[y][x];
            for (int y = sy0; y <= sy1; y++) {
                int a = Math.max(sy0, y - reach);
                int b = Math.min(sy1, y + reach);
                column[y - sy0] = (sums[b - sy0 + 1] - sums[a - sy0]) / (b - a + 1);
            }
            for (int y = y0; y <= y1; y++)
                scratch[y][x] = column[y - sy0];
        }
    }

    // ---- After the generator ----

    /**
     * Pushes the ground along a swirling flow (sign 1), or against it (sign -1), as the generator perturbs its height
     * map: coasts and slopes bend and wander instead of running straight. The flow is fixed for a stroke.
     */
    void applyWarp(float cx, float cy, float radius, float intensity, int sign, float dt, int seed) {
        float feature = Math.max(4f, radius / 2f);
        float step = sign * intensity * WARP_RATE * dt;
        int x0 = Math.max(0, (int) Math.floor(cx - radius) - 2);
        int y0 = Math.max(0, (int) Math.floor(cy - radius) - 2);
        int x1 = Math.min(size - 1, (int) Math.ceil(cx + radius) + 2);
        int y1 = Math.min(size - 1, (int) Math.ceil(cy + radius) + 2);
        for (int y = y0; y <= y1; y++)
            System.arraycopy(heights[y], x0, scratch[y], x0, x1 - x0 + 1);
        forEachCell(cx, cy, radius, (x, y, weight) -> {
            // Noise in [-1, 1] each way; one octave fewer than the bumps keeps the flow smooth.
            float fx = 2f * Noise.fractal(x, y, feature, 2, seed) - 1f;
            float fy = 2f * Noise.fractal(x, y, feature, 2, seed + 31) - 1f;
            // Each frame moves the ground less than a cell, so the samples stay inside the copied square.
            float sx = Math.clamp(x - fx * step * weight * 2f, x0, x1);
            float sy = Math.clamp(y - fy * step * weight * 2f, y0, y1);
            return sampleScratch(sx, sy, x1, y1);
        });
    }

    private float sampleScratch(float x, float y, int max_x, int max_y) {
        int x0 = Math.min((int) x, max_x - 1);
        int y0 = Math.min((int) y, max_y - 1);
        float fx = x - x0;
        float fy = y - y0;
        float a = scratch[y0][x0] * (1 - fx) + scratch[y0][x0 + 1] * fx;
        float b = scratch[y0 + 1][x0] * (1 - fx) + scratch[y0 + 1][x0 + 1] * fx;
        return a * (1 - fy) + b * fy;
    }

    // ---- Moving the ground about ----

    /**
     * Turns the ground under the brush about its middle, anticlockwise (sign 1) or clockwise (sign -1) seen from
     * above. The middle of the brush turns as one piece and the ground round it shears into the ground outside.
     */
    void applyTwist(float cx, float cy, float radius, float intensity, int sign, float dt) {
        turn(cx, cy, radius, radius * TWIST_CORE, sign * intensity * TWIST_RATE * dt);
    }

    /**
     * Swirls the ground under the brush like a whirlpool, anticlockwise (sign 1) or clockwise (sign -1) seen from
     * above. The very middle spins fastest and the edge hardly at all, so whatever lies across the brush winds into a
     * spiral.
     */
    void applySwirl(float cx, float cy, float radius, float intensity, int sign, float dt) {
        turn(cx, cy, radius, 0f, sign * intensity * SWIRL_RATE * dt);
    }

    /** Turns the ground within a radius of a point by an angle in radians out to the core distance, less beyond. */
    private void turn(float cx, float cy, float radius, float core, float angle) {
        if (angle == 0f)
            return;
        moveGround(cx, cy, radius, true, 0f, (x, y, back) -> {
            float dx = x - cx;
            float dy = y - cy;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance >= radius)
                return false;
            // The ground here came from where turning it back takes it.
            double turned = -angle * plateau(distance, radius, core);
            float cos = (float) Math.cos(turned);
            float sin = (float) Math.sin(turned);
            back[0] = cx + dx * cos - dy * sin;
            back[1] = cy + dx * sin + dy * cos;
            return true;
        });
    }

    /**
     * Drags the ground under the brush from one point to another like putty: the ground grabbed at the first point
     * follows to the second, the ground behind it stretches out and the ground ahead bunches up. The ground is skewed
     * up (sign 1) or down (sign -1) as it goes, by how far it ends up from where it lay, so the ground grabbed rises
     * or sinks most and the stretch behind it slopes back to the ground it left. A long drag goes in short steps, so
     * the ground never folds over itself.
     *
     * @param intensity how much of the brush holds together as one piece, from its very middle to most of it
     */
    void applyStretch(float ax, float ay, float bx, float by, float radius, float intensity, int sign) {
        float core = radius * STRETCH_CORE * intensity;
        float dx = bx - ax;
        float dy = by - ay;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f)
            return;
        // Past the core the weight falls off at most pi / 2 per falloff width; a step must stay well under the
        // inverse of that, or ground behind would overtake the ground ahead of it.
        int steps = Math.clamp((int) Math.ceil(length / Math.max(.1f, .4f * (radius - core))), 1,
                MAX_STRETCH_STEPS);
        float sx = dx / steps;
        float sy = dy / steps;
        for (int i = 1; i <= steps; i++) {
            float cx = ax + sx * i;
            float cy = ay + sy * i;
            moveGround(cx, cy, radius, false, sign * STRETCH_SKEW, (x, y, back) -> {
                float distance = (float) Math.hypot(x - cx, y - cy);
                if (distance >= radius)
                    return false;
                float weight = plateau(distance, radius, core);
                back[0] = x - sx * weight;
                back[1] = y - sy * weight;
                return true;
            });
        }
    }

    /** A value of a grid as large as the map at a grid position, through a Catmull-Rom spline. */
    private float sampleCubic(float @NonNull [] @NonNull [] grid, float gx, float gy) {
        gx = Math.clamp(gx, 0f, size - 1);
        gy = Math.clamp(gy, 0f, size - 1);
        int x0 = Math.min((int) gx, size - 2);
        int y0 = Math.min((int) gy, size - 2);
        float fx = gx - x0;
        float fy = gy - y0;
        return catmullRom(along(grid[Math.max(0, y0 - 1)], x0, fx), along(grid[y0], x0, fx),
                along(grid[y0 + 1], x0, fx), along(grid[Math.min(size - 1, y0 + 2)], x0, fx), fy);
    }

    /** A row's value a share of the way from one cell to the next, through a Catmull-Rom spline. */
    private float along(float @NonNull [] row, int x0, float fx) {
        return catmullRom(row[Math.max(0, x0 - 1)], row[x0], row[x0 + 1], row[Math.min(size - 1, x0 + 2)], fx);
    }

    private static float catmullRom(float a, float b, float c, float d, float t) {
        return b + .5f * t * (c - a + t * (2f * a - 5f * b + 4f * c - d + t * (3f * (b - c) + d - a)));
    }

    @FunctionalInterface
    private interface Motion {
        /** Puts in back where the ground now in a cell lay before this move, or returns false if it stays put. */
        boolean from(int x, int y, float @NonNull [] back);
    }

    /**
     * Moves the ground within a radius of a point. Each move is added to where each cell's ground lay when the stroke
     * began, and its height read from there again, so the ground stays as sharp as it was however long it is turned
     * or dragged about, rather than blurring a little more every frame.
     *
     * @param cubic whether to read the origins through a spline. Ground turned round and round winds them into tight
     *              but smooth spirals, which bilinear reading lets drift a little every frame. Ground dragged along
     *              piles up
     *              against the front of the brush, where a spline would overshoot and fold the ground over itself.
     * @param skew  meters the ground rises for each grid unit it ends up from where it lay, or sinks when below 0
     */
    private void moveGround(float cx, float cy, float radius, boolean cubic, float skew,
            @NonNull Motion motion) {
        float[][] start = stroke_backup;
        if (start == null)
            return;
        int x0 = Math.max(0, (int) Math.floor(cx - radius));
        int y0 = Math.max(0, (int) Math.floor(cy - radius));
        int x1 = Math.min(size - 1, (int) Math.ceil(cx + radius));
        int y1 = Math.min(size - 1, (int) Math.ceil(cy + radius));
        if (x0 > x1 || y0 > y1)
            return;
        float[][] from_x = origin_x;
        float[][] from_y = origin_y;
        float[][] next_y = scratch_y;
        if (from_x == null || from_y == null || next_y == null) {
            from_x = origin_x = new float[size][size];
            from_y = origin_y = new float[size][size];
            next_y = scratch_y = new float[size][size];
        }
        if (!origins_ready) {
            for (int y = 0; y < size; y++) {
                Arrays.fill(from_y[y], y);
                for (int x = 0; x < size; x++)
                    from_x[y][x] = x;
            }
            origins_ready = true;
        }
        // The new origins go to scratch first, so every cell reads the origins from before this move.
        float[] back = new float[2];
        boolean moved = false;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!motion.from(x, y, back)) {
                    scratch[y][x] = Float.NaN;
                    continue;
                }
                scratch[y][x] = cubic ? sampleCubic(from_x, back[0], back[1]) : sample(from_x, back[0], back[1]);
                next_y[y][x] = cubic ? sampleCubic(from_y, back[0], back[1]) : sample(from_y, back[0], back[1]);
                moved = true;
            }
        }
        if (!moved)
            return;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float ox = scratch[y][x];
                if (Float.isNaN(ox))
                    continue;
                float oy = next_y[y][x];
                from_x[y][x] = ox;
                from_y[y][x] = oy;
                float rise = skew != 0f ? skew * (float) Math.hypot(x - ox, y - oy) : 0f;
                heights[y][x] = clampHeight(x, y, sample(start, ox, oy) + rise);
            }
        }
        markDirty(x0, y0, x1, y1);
    }

    /**
     * Raises (sign 1) or lowers (sign -1) the ground in cells, as the generator's Voronoi cliffs do: the middle of
     * each cell moves as one, its borders hardly at all, so the ground breaks into mesas and basins with cliffs
     * between them. The cells are fixed for a stroke, about half the brush across.
     */
    void applyCliffs(float cx, float cy, float radius, float intensity, int sign, float dt, int seed) {
        float amount = sign * intensity * RAISE_RATE * .5f * dt;
        float cell = Math.max(4f, radius / 2f);
        forEachCell(cx, cy, radius, (x, y, weight) -> heights[y][x] + amount * weight * Math.min(1f, 3f * Noise.cells(
                x / cell, y / cell, seed)));
    }

    /**
     * Erodes the ground. Sign 1 is the generator's erosion: every slope gentle enough to walk settles towards its
     * lowest neighbour while steeper ones stay, so the ground parts into level shelves between cliffs. Sign -1 is
     * weathering: every slope too steep to walk slumps until it can be walked, wearing cliffs down into scree.
     */
    void applyErode(float cx, float cy, float radius, float intensity, int sign, float dt) {
        float rate = Math.min(1f, intensity * ERODE_RATE * dt);
        // Channel.erode works on slopes up to its talus; the generator's talus is about one and a half walkable steps.
        float settle = walk_step * 1.5f;
        float slump = walk_step * .9f;
        int x0 = Math.max(1, (int) Math.floor(cx - radius));
        int y0 = Math.max(1, (int) Math.floor(cy - radius));
        int x1 = Math.min(size - 2, (int) Math.ceil(cx + radius));
        int y1 = Math.min(size - 2, (int) Math.ceil(cy + radius));
        if (x0 > x1 || y0 > y1)
            return;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float distance = (float) Math.hypot(x - cx, y - cy);
                if (distance >= radius)
                    continue;
                float h = heights[y][x];
                // The lowest of the four neighbours, as Channel.erode looks.
                int nx = x;
                int ny = y;
                float drop = 0f;
                for (int k = 0; k < NEIGHBOURS.length; k += 2) {
                    int ox = x + NEIGHBOURS[k];
                    int oy = y + NEIGHBOURS[k + 1];
                    if (h - heights[oy][ox] > drop) {
                        drop = h - heights[oy][ox];
                        nx = ox;
                        ny = oy;
                    }
                }
                float moved;
                if (sign > 0)
                    moved = drop > 0f && drop <= settle ? drop * .5f : 0f;
                else
                    moved = drop > slump ? (drop - slump) * .5f : 0f;
                if (moved == 0f)
                    continue;
                moved *= rate * falloff(distance, radius);
                heights[y][x] = clampHeight(x, y, h - moved);
                heights[ny][nx] = clampHeight(nx, ny, heights[ny][nx] + moved);
            }
        }
        markDirty(x0 - 1, y0 - 1, x1 + 1, y1 + 1);
    }

    /**
     * Shapes the shore. Sign 1 lays beaches as the generator does: the ground near the sea eases into gentle sand
     * slopes and shallows. Sign -1 makes a cliff coast: low land by the sea is raised to a cliff top and the water
     * below it deepened. Both work from the heights the stroke began with, so holding the button only blends further.
     */
    void applyBeach(float cx, float cy, float radius, float intensity, int sign, float dt) {
        float[][] start = stroke_backup;
        if (start == null)
            return;
        float blend = Math.min(1f, intensity * ERODE_RATE * .25f * dt);
        // Landscape.beaches, in meters: its sea level is a tenth above the real one.
        float shore = 1.1f * sea_level;
        float top = 2f * shore;
        forEachCell(cx, cy, radius, (x, y, weight) -> {
            float h = heights[y][x];
            float before = start[y][x];
            float target;
            if (sign > 0) {
                if (before < shore)
                    target = Tools.interpolateSmooth(0f, shore, before / shore);
                else if (before < top)
                    target = Tools.interpolateSmooth(shore, 2f * top - shore, .5f * (before - shore) / (top - shore));
                else
                    target = before;
            } else {
                target = before >= sea_level ? Math.max(before, 2f * sea_level) : Math.min(before,
                        sea_level - SHALLOW_DEPTH);
            }
            return h + (target - h) * blend * weight;
        });
    }

    // ---- Copy and paste ----

    /** The heights in a rectangle of cells (inclusive, within the map), a row at a time. */
    float @NonNull [] @NonNull [] copyRect(int x0, int y0, int x1, int y1) {
        float[][] copy = new float[y1 - y0 + 1][];
        for (int y = y0; y <= y1; y++)
            copy[y - y0] = Arrays.copyOfRange(heights[y], x0, x1 + 1);
        return copy;
    }

    /**
     * Lays copied heights with their first cell on (x0, y0), blended in over the outer cells; intensity blends all of
     * them. Unless kept at the heights they were copied at, they are lifted or lowered as a whole so their border
     * meets the ground they land on, on average.
     */
    void paste(float @NonNull [] @NonNull [] source, int x0, int y0, float intensity, boolean keep_heights) {
        int h = source.length;
        int w = source[0].length;
        float source_border = 0f;
        float target_border = 0f;
        int count = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (j != 0 && j != h - 1 && i != 0 && i != w - 1)
                    continue;
                int x = x0 + i;
                int y = y0 + j;
                if (x < 0 || y < 0 || x >= size || y >= size)
                    continue;
                source_border += source[j][i];
                target_border += heights[y][x];
                count++;
            }
        }
        float offset = count > 0 && !keep_heights ? (target_border - source_border) / count : 0f;
        float feather = Math.max(2f, Math.min(w, h) * .15f);
        int cx0 = Math.max(0, x0), cy0 = Math.max(0, y0);
        int cx1 = Math.min(size - 1, x0 + w - 1), cy1 = Math.min(size - 1, y0 + h - 1);
        if (cx0 > cx1 || cy0 > cy1)
            return;
        for (int y = cy0; y <= cy1; y++) {
            for (int x = cx0; x <= cx1; x++) {
                int i = x - x0;
                int j = y - y0;
                float edge = Math.min(Math.min(i, w - 1 - i), Math.min(j, h - 1 - j));
                float weight = edge >= feather ? 1f : Noise.smooth(edge / feather);
                float ground = heights[y][x];
                heights[y][x] = clampHeight(x, y, ground + (source[j][i] + offset - ground) * weight * intensity);
            }
        }
        markDirty(cx0, cy0, cx1, cy1);
    }

    @FunctionalInterface
    private interface CellFunction {
        float apply(int x, int y, float weight);
    }

    private void forEachCell(float cx, float cy, float radius, @NonNull CellFunction function) {
        forEachCell(cx, cy, radius, 0f, function);
    }

    /** @param core share of the radius where the weight stays at 1 before falling off */
    private void forEachCell(float cx, float cy, float radius, float core, @NonNull CellFunction function) {
        int x0 = Math.max(0, (int) Math.floor(cx - radius));
        int y0 = Math.max(0, (int) Math.floor(cy - radius));
        int x1 = Math.min(size - 1, (int) Math.ceil(cx + radius));
        int y1 = Math.min(size - 1, (int) Math.ceil(cy + radius));
        if (x0 > x1 || y0 > y1)
            return;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance >= radius)
                    continue;
                heights[y][x] = clampHeight(x, y, function.apply(x, y, plateau(distance, radius, radius * core)));
            }
        }
        markDirty(x0, y0, x1, y1);
    }

    /** Smooth bell from 1 at the center to 0 at the edge. */
    private static float falloff(float distance, float radius) {
        return 0.5f * (1f + (float) Math.cos(Math.PI * distance / radius));
    }

    /** 1 out to the core distance, then a smooth bell down to 0 at the edge. */
    private static float plateau(float distance, float radius, float core) {
        return distance <= core ? 1f : falloff(distance - core, radius - core);
    }

    private float localAverage(int x, int y) {
        // 3x3 tent filter: 4 for the center, 2 for edges, 1 for corners.
        float sum = 0f;
        float total = 0f;
        for (int oy = -1; oy <= 1; oy++) {
            int sy = Math.clamp(y + oy, 0, size - 1);
            for (int ox = -1; ox <= 1; ox++) {
                int sx = Math.clamp(x + ox, 0, size - 1);
                float w = (2 - Math.abs(ox)) * (2 - Math.abs(oy));
                sum += scratch[sy][sx] * w;
                total += w;
            }
        }
        return sum / total;
    }

    /**
     * Keeps a cell's height in range, and the outermost cells on the sea floor. The generator keeps them there, and
     * the world wraps: the last row of the landscape takes its heights from the first, so a raised edge tears open
     * into a wall that shows on the far side of the map.
     */
    private float clampHeight(int x, int y, float h) {
        if (x == 0 || y == 0 || x == size - 1 || y == size - 1)
            return MIN_HEIGHT;
        return Math.clamp(h, MIN_HEIGHT, MAX_HEIGHT);
    }

    /** Puts the outermost cells on the sea floor, as the generator does, for heights saved before it was kept. */
    static void pinEdges(float @NonNull [] @NonNull [] heights) {
        int last = heights.length - 1;
        for (int i = 0; i <= last; i++) {
            heights[0][i] = MIN_HEIGHT;
            heights[last][i] = MIN_HEIGHT;
            heights[i][0] = MIN_HEIGHT;
            heights[i][last] = MIN_HEIGHT;
        }
    }

    // ---- Pushing changes to the renderer ----

    private void markDirty(int x0, int y0, int x1, int y1) {
        modified = true;
        markShown(x0, y0, x1, y1);
        if (stroke_backup != null) {
            stroke_x0 = Math.min(stroke_x0, x0);
            stroke_y0 = Math.min(stroke_y0, y0);
            stroke_x1 = Math.max(stroke_x1, x1);
            stroke_y1 = Math.max(stroke_y1, y1);
        }
    }

    /** Marks heights to upload on the next flush. */
    private void markShown(int x0, int y0, int x1, int y1) {
        dirty_x0 = Math.min(dirty_x0, x0);
        dirty_y0 = Math.min(dirty_y0, y0);
        dirty_x1 = Math.max(dirty_x1, x1);
        dirty_y1 = Math.max(dirty_y1, y1);
    }

    /** Uploads everything changed since the last flush. Must run on the render thread. */
    void flush() {
        if (dirty_x0 > dirty_x1)
            return;
        int x0 = dirty_x0;
        int y0 = dirty_y0;
        int x1 = dirty_x1;
        int y1 = dirty_y1;
        dirty_x0 = Integer.MAX_VALUE;
        dirty_y0 = Integer.MAX_VALUE;
        dirty_x1 = Integer.MIN_VALUE;
        dirty_y1 = Integer.MIN_VALUE;

        // The render context caches texture bindings, so leave the binding as we found it.
        int previous_texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            uploadRect(x0, y0, x1, y1);
            growPatchBounds(x0, y0, x1, y1);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous_texture);
        }
        listener.heightsChanged(x0, y0, x1, y1);
    }

    private void uploadRect(int x0, int y0, int x1, int y1) {
        int w = x1 - x0 + 1;
        int h = y1 - y0 + 1;
        upload.clear();
        for (int y = y0; y <= y1; y++)
            upload.put(heights[y], x0, w);
        upload.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, height_map.getHeightTexture().getHandle());
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x0, y0, w, h, GL11.GL_RED, GL11.GL_FLOAT, upload);
    }

    private void growPatchBounds(int x0, int y0, int x1, int y1) {
        int cells = HeightMap.GRID_UNITS_PER_PATCH;
        int patches = size / cells;
        float meters_per_cell = HeightMap.METERS_PER_UNIT_GRID;
        // A cell on a patch edge is shared with the patch before it, so widen the range by one patch.
        int px0 = Math.max(0, (x0 - 1) / cells);
        int py0 = Math.max(0, (y0 - 1) / cells);
        int px1 = Math.min(patches - 1, x1 / cells);
        int py1 = Math.min(patches - 1, y1 / cells);
        for (int py = py0; py <= py1; py++) {
            for (int px = px0; px <= px1; px++) {
                LandscapeLeaf leaf = height_map.getLeafFromCoordinates((px + .5f) * cells * meters_per_cell,
                        (py + .5f) * cells * meters_per_cell);
                int min_x = -1, min_y = -1, max_x = -1, max_y = -1;
                float min = Float.POSITIVE_INFINITY;
                float max = Float.NEGATIVE_INFINITY;
                int cx1 = Math.min(size - 1, (px + 1) * cells);
                int cy1 = Math.min(size - 1, (py + 1) * cells);
                for (int y = py * cells; y <= cy1; y++) {
                    for (int x = px * cells; x <= cx1; x++) {
                        float h = heights[y][x];
                        if (h < min) {
                            min = h;
                            min_x = x;
                            min_y = y;
                        }
                        if (h > max) {
                            max = h;
                            max_x = x;
                            max_y = y;
                        }
                    }
                }
                // editHeight writes the value that is already there; we only want its bounds bookkeeping.
                if (min < leaf.bmin_z)
                    height_map.editHeight(min_x, min_y, min);
                if (max > leaf.bmax_z)
                    height_map.editHeight(max_x, max_y, max);
            }
        }
    }
}
