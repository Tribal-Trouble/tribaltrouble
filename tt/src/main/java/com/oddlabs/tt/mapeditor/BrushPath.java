package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * A course laid out by clicks, as a smooth curve through them, and how far each cell lies from it.
 *
 * <p>The curve is a Catmull-Rom spline through the clicked points, cut into short straight pieces. Every cell near it
 * learns its distance to the nearest piece and how far along the course that nearest point is, which the river and
 * ridge brushes shape their cross section and their changes along the way by.
 */
final class BrushPath {
    /** Length of the straight pieces the curve is cut into, in grid units. */
    private static final float STEP = 1f;

    private final float @NonNull [] xs;
    private final float @NonNull [] ys;
    // Distance along the course to each point of the curve.
    private final float @NonNull [] arc;
    // Distance along the course to each clicked point.
    private final float @NonNull [] knots;

    /** @param points the clicked points, in grid units */
    BrushPath(@NonNull List<float @NonNull []> points) {
        if (points.isEmpty())
            throw new IllegalArgumentException("A course needs a point");
        int count = 1;
        int[] pieces = new int[points.size()];
        for (int i = 0; i + 1 < points.size(); i++) {
            float[] a = points.get(i);
            float[] b = points.get(i + 1);
            pieces[i] = Math.max(1, (int) Math.ceil(Math.hypot(b[0] - a[0], b[1] - a[1]) / STEP));
            count += pieces[i];
        }
        xs = new float[count];
        ys = new float[count];
        arc = new float[count];
        knots = new float[points.size()];
        xs[0] = points.getFirst()[0];
        ys[0] = points.getFirst()[1];
        int n = 1;
        for (int i = 0; i + 1 < points.size(); i++) {
            // The ends are repeated, so the curve leaves the first point and reaches the last head on.
            float[] p0 = points.get(Math.max(0, i - 1));
            float[] p1 = points.get(i);
            float[] p2 = points.get(i + 1);
            float[] p3 = points.get(Math.min(points.size() - 1, i + 2));
            for (int j = 1; j <= pieces[i]; j++) {
                float t = j / (float) pieces[i];
                xs[n] = catmullRom(p0[0], p1[0], p2[0], p3[0], t);
                ys[n] = catmullRom(p0[1], p1[1], p2[1], p3[1], t);
                arc[n] = arc[n - 1] + (float) Math.hypot(xs[n] - xs[n - 1], ys[n] - ys[n - 1]);
                n++;
            }
            knots[i + 1] = arc[n - 1];
        }
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return .5f * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (3 * p1 - p0 - 3 * p2 + p3) * t3);
    }

    /** Length of the course, in grid units. */
    float length() {
        return arc[arc.length - 1];
    }

    /** Grid units along the course to each clicked point, the first at 0 and the last at {@link #length}. */
    float @NonNull [] knots() {
        return knots.clone();
    }

    /** Grid units along the course to a point of the smoothed curve, counted as {@link #curve} lists them. */
    float along(int point) {
        return arc[point];
    }

    /** The points of the smoothed curve, x and y in turn, in grid units. */
    float @NonNull [] curve() {
        float[] curve = new float[xs.length * 2];
        for (int i = 0; i < xs.length; i++) {
            curve[2 * i] = xs[i];
            curve[2 * i + 1] = ys[i];
        }
        return curve;
    }

    /** Told about a cell near the course. */
    @FunctionalInterface
    interface CellVisitor {
        /**
         * @param distance grid units from the cell to the course
         * @param along    grid units along the course to the point nearest the cell
         */
        void visit(int x, int y, float distance, float along);
    }

    /** Visits every cell of a size by size map within a reach of the course. */
    void forEachCellWithin(float reach, int size, @NonNull CellVisitor visitor) {
        float min_x = Float.POSITIVE_INFINITY, min_y = Float.POSITIVE_INFINITY;
        float max_x = Float.NEGATIVE_INFINITY, max_y = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < xs.length; i++) {
            min_x = Math.min(min_x, xs[i]);
            min_y = Math.min(min_y, ys[i]);
            max_x = Math.max(max_x, xs[i]);
            max_y = Math.max(max_y, ys[i]);
        }
        int x0 = Math.max(0, (int) Math.floor(min_x - reach));
        int y0 = Math.max(0, (int) Math.floor(min_y - reach));
        int x1 = Math.min(size - 1, (int) Math.ceil(max_x + reach));
        int y1 = Math.min(size - 1, (int) Math.ceil(max_y + reach));
        if (x0 > x1 || y0 > y1)
            return;
        int w = x1 - x0 + 1;
        int h = y1 - y0 + 1;
        float[] nearest = new float[w * h];
        float[] along = new float[w * h];
        Arrays.fill(nearest, Float.POSITIVE_INFINITY);
        float reach_sq = reach * reach;
        // Each piece only looks at the cells around it, so a long course over a small brush stays cheap.
        int pieces = Math.max(1, xs.length - 1);
        for (int i = 0; i < pieces; i++) {
            int j = Math.min(i + 1, xs.length - 1);
            float ax = xs[i], ay = ys[i];
            float dx = xs[j] - ax, dy = ys[j] - ay;
            float length_sq = dx * dx + dy * dy;
            int px0 = Math.max(x0, (int) Math.floor(Math.min(ax, xs[j]) - reach));
            int py0 = Math.max(y0, (int) Math.floor(Math.min(ay, ys[j]) - reach));
            int px1 = Math.min(x1, (int) Math.ceil(Math.max(ax, xs[j]) + reach));
            int py1 = Math.min(y1, (int) Math.ceil(Math.max(ay, ys[j]) + reach));
            for (int y = py0; y <= py1; y++) {
                for (int x = px0; x <= px1; x++) {
                    float t = length_sq > 0f ? Math.clamp(((x - ax) * dx + (y - ay) * dy) / length_sq, 0f, 1f) : 0f;
                    float ex = ax + dx * t - x;
                    float ey = ay + dy * t - y;
                    float d_sq = ex * ex + ey * ey;
                    int k = (y - y0) * w + (x - x0);
                    if (d_sq < reach_sq && d_sq < nearest[k]) {
                        nearest[k] = d_sq;
                        along[k] = arc[i] + (arc[j] - arc[i]) * t;
                    }
                }
            }
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int k = (y - y0) * w + (x - x0);
                if (nearest[k] != Float.POSITIVE_INFINITY)
                    visitor.visit(x, y, (float) Math.sqrt(nearest[k]), along[k]);
            }
        }
    }
}
