package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;

/**
 * Keeps one player's island in step with the others in a shared session.
 *
 * <p>Besides the island as it is shown, this keeps the island as the session has it: every edit laid in the order
 * the server put them, followed by this player's own edits that are on their way. A cell shown differently from that
 * is one this player changed and has not sent yet, and {@link #take} turns those into the next edit.
 *
 * <p>Another player's edit comes before this player's own edits still on their way, which the server will put after
 * it. So it goes into the session's island, those edits go over it again, and the result is shown wherever this
 * player has nothing unsent. Every player then ends with the same island once the edits stop, without waiting for
 * the server to show their own edits.
 */
final class SessionSync {
    /** The heights as the editor holds them. */
    interface Ground {
        /** The heights the editor shows and edits, a row at a time, written in place. */
        float @NonNull [] @NonNull [] heights();

        /** Heights in a rectangle (inclusive) were rounded to what an edit carries; the renderer should follow. */
        void rounded(int x0, int y0, int x1, int y1);

        /**
         * Lays heights another player's edit brought, a row at a time, leaving a cell alone where its value is NaN.
         * Undo should then leave them standing.
         */
        void applyShared(int x0, int y0, int width, int height, float @NonNull [] values);
    }

    /** The resources as the editor holds them, by cell as y * size + x and kind as a {@link Resource} ordinal or -1. */
    interface Supplies {
        byte kind(int cell);

        /** Puts the given kinds on the first count cells, whatever stands there and whatever painting allows. */
        void applyShared(int @NonNull [] cells, byte @NonNull [] kinds, int count);
    }

    /**
     * Where the editor tells of changed heights and resources, built with it before there is a session to pass them
     * on to.
     */
    static final class Link {
        private @Nullable SessionSync sync;

        void set(@Nullable SessionSync sync) {
            this.sync = sync;
        }

        void heightsChanged(int x0, int y0, int x1, int y1) {
            if (sync != null)
                sync.heightsChanged(x0, y0, x1, y1);
        }

        void resourcesChanged(int x0, int y0, int x1, int y1) {
            if (sync != null)
                sync.resourcesChanged(x0, y0, x1, y1);
        }
    }

    private final @NonNull Ground ground;
    private final @NonNull Supplies supplies;
    private final int size;
    // The island as the session has it, this player's own edits on their way included.
    private final float @NonNull [] @NonNull [] shared;
    private final byte @NonNull [] shared_kinds;
    // This player's edits sent and not yet acknowledged, oldest first.
    private final Deque<@NonNull EditOp> pending = new ArrayDeque<>();
    private final @NonNull BitSet in_edit;

    // Where heights and resources may have changed since the last edit was taken (inclusive).
    private int hx0 = Integer.MAX_VALUE;
    private int hy0 = Integer.MAX_VALUE;
    private int hx1 = Integer.MIN_VALUE;
    private int hy1 = Integer.MIN_VALUE;
    private int rx0 = Integer.MAX_VALUE;
    private int ry0 = Integer.MAX_VALUE;
    private int rx1 = Integer.MIN_VALUE;
    private int ry1 = Integer.MIN_VALUE;

    /** Starts from the island as shown, which must be the island as the session has it. */
    SessionSync(@NonNull Ground ground, @NonNull Supplies supplies, int size) {
        this.ground = ground;
        this.supplies = supplies;
        this.size = size;
        float[][] heights = ground.heights();
        shared = new float[size][];
        for (int y = 0; y < size; y++)
            shared[y] = heights[y].clone();
        shared_kinds = new byte[size * size];
        for (int cell = 0; cell < shared_kinds.length; cell++)
            shared_kinds[cell] = supplies.kind(cell);
        in_edit = new BitSet(size * size);
    }

    int getSize() {
        return size;
    }

    /** Notes a rectangle of cells (inclusive) whose heights may have changed. */
    void heightsChanged(int x0, int y0, int x1, int y1) {
        hx0 = Math.min(hx0, x0);
        hy0 = Math.min(hy0, y0);
        hx1 = Math.max(hx1, x1);
        hy1 = Math.max(hy1, y1);
    }

    /** Notes a rectangle of cells (inclusive) where resources may have come or gone. */
    void resourcesChanged(int x0, int y0, int x1, int y1) {
        rx0 = Math.min(rx0, x0);
        ry0 = Math.min(ry0, y0);
        rx1 = Math.max(rx1, x1);
        ry1 = Math.max(ry1, y1);
    }

    /** Notes the whole island as possibly changed, so the next {@link #take} finds every change not yet sent. */
    void changedAnywhere() {
        heightsChanged(0, 0, size - 1, size - 1);
        resourcesChanged(0, 0, size - 1, size - 1);
    }

    /**
     * Takes what this player changed since the last edit as a new edit, to send. It is kept until acknowledged.
     * Changed heights are rounded to what an edit carries, here as well.
     *
     * @return the edit, or null when nothing changed
     */
    @Nullable
    EditOp take() {
        EditOp op = takeHeights();
        int[] cells = new int[0];
        byte[] kinds = new byte[0];
        if (rx0 <= rx1) {
            int count = 0;
            int x0 = Math.max(0, rx0), y0 = Math.max(0, ry0);
            int x1 = Math.min(size - 1, rx1), y1 = Math.min(size - 1, ry1);
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    int cell = y * size + x;
                    byte kind = supplies.kind(cell);
                    if (kind == shared_kinds[cell])
                        continue;
                    if (count == cells.length) {
                        cells = Arrays.copyOf(cells, Math.max(16, count * 2));
                        kinds = Arrays.copyOf(kinds, cells.length);
                    }
                    cells[count] = cell;
                    kinds[count++] = kind;
                    shared_kinds[cell] = kind;
                }
            }
            cells = Arrays.copyOf(cells, count);
            kinds = Arrays.copyOf(kinds, count);
            rx0 = ry0 = Integer.MAX_VALUE;
            rx1 = ry1 = Integer.MIN_VALUE;
        }
        if (op == null && cells.length == 0)
            return null;
        EditOp edit = op == null ? new EditOp(0, 0, 0, 0, new float[0], cells, kinds) : new EditOp(op.x0(), op.y0(),
                op.width(), op.height(), op.heights(), cells, kinds);
        pending.add(edit);
        return edit;
    }

    private @Nullable EditOp takeHeights() {
        if (hx0 > hx1)
            return null;
        int x0 = Math.max(0, hx0), y0 = Math.max(0, hy0);
        int x1 = Math.min(size - 1, hx1), y1 = Math.min(size - 1, hy1);
        hx0 = hy0 = Integer.MAX_VALUE;
        hx1 = hy1 = Integer.MIN_VALUE;
        float[][] heights = ground.heights();
        // The cells shown differently from the session's island.
        int bx0 = Integer.MAX_VALUE, by0 = Integer.MAX_VALUE, bx1 = Integer.MIN_VALUE, by1 = Integer.MIN_VALUE;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (heights[y][x] != shared[y][x]) {
                    bx0 = Math.min(bx0, x);
                    by0 = Math.min(by0, y);
                    bx1 = Math.max(bx1, x);
                    by1 = Math.max(by1, y);
                }
            }
        }
        if (bx0 > bx1)
            return null;
        int width = bx1 - bx0 + 1;
        int height = by1 - by0 + 1;
        float[] values = new float[width * height];
        Arrays.fill(values, Float.NaN);
        boolean any = false;
        boolean rounded = false;
        for (int y = by0; y <= by1; y++) {
            for (int x = bx0; x <= bx1; x++) {
                float h = heights[y][x];
                if (h == shared[y][x])
                    continue;
                float q = EditOp.quantize(h);
                heights[y][x] = q;
                rounded |= q != h;
                if (q != shared[y][x]) {
                    values[(y - by0) * width + x - bx0] = q;
                    shared[y][x] = q;
                    any = true;
                }
            }
        }
        if (rounded)
            ground.rounded(bx0, by0, bx1, by1);
        return any ? new EditOp(bx0, by0, width, height, values, new int[0], new byte[0]) : null;
    }

    /** The server put this player's oldest edit on its way in the session's order. */
    void acknowledged() {
        pending.poll();
    }

    /** Lays another player's edit, which the server put before this player's edits still on their way. */
    void apply(@NonNull EditOp op) {
        if (op.hasHeights())
            applyHeights(op);
        if (op.resource_cells().length > 0)
            applyResources(op);
    }

    private void applyHeights(@NonNull EditOp op) {
        int x0 = op.x0(), y0 = op.y0(), width = op.width(), height = op.height();
        float[][] heights = ground.heights();
        float[] values = op.heights();
        // Cells this player changed and has not sent stay as they are: they go after the edit.
        boolean[] unsent = new boolean[width * height];
        for (int j = 0; j < height; j++)
            for (int i = 0; i < width; i++)
                unsent[j * width + i] = heights[y0 + j][x0 + i] != shared[y0 + j][x0 + i];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float v = values[j * width + i];
                if (!Float.isNaN(v))
                    shared[y0 + j][x0 + i] = v;
            }
        }
        for (EditOp own : pending) {
            if (!own.hasHeights())
                continue;
            int ix0 = Math.max(x0, own.x0()), iy0 = Math.max(y0, own.y0());
            int ix1 = Math.min(x0 + width, own.x0() + own.width()) - 1;
            int iy1 = Math.min(y0 + height, own.y0() + own.height()) - 1;
            for (int y = iy0; y <= iy1; y++) {
                for (int x = ix0; x <= ix1; x++) {
                    float v = own.heights()[(y - own.y0()) * own.width() + x - own.x0()];
                    if (!Float.isNaN(v))
                        shared[y][x] = v;
                }
            }
        }
        float[] shown = new float[width * height];
        boolean any = false;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float v = shared[y0 + j][x0 + i];
                boolean change = !unsent[j * width + i] && heights[y0 + j][x0 + i] != v;
                shown[j * width + i] = change ? v : Float.NaN;
                any |= change;
            }
        }
        if (any)
            ground.applyShared(x0, y0, width, height, shown);
    }

    private void applyResources(@NonNull EditOp op) {
        int[] cells = op.resource_cells();
        byte[] kinds = op.resource_kinds();
        boolean[] unsent = new boolean[cells.length];
        for (int i = 0; i < cells.length; i++) {
            unsent[i] = supplies.kind(cells[i]) != shared_kinds[cells[i]];
            in_edit.set(cells[i]);
        }
        for (int i = 0; i < cells.length; i++)
            shared_kinds[cells[i]] = kinds[i];
        for (EditOp own : pending) {
            int[] own_cells = own.resource_cells();
            for (int i = 0; i < own_cells.length; i++)
                if (in_edit.get(own_cells[i]))
                    shared_kinds[own_cells[i]] = own.resource_kinds()[i];
        }
        int[] changed = new int[cells.length];
        byte[] changed_kinds = new byte[cells.length];
        int count = 0;
        for (int i = 0; i < cells.length; i++) {
            int cell = cells[i];
            in_edit.clear(cell);
            if (!unsent[i] && supplies.kind(cell) != shared_kinds[cell]) {
                changed[count] = cell;
                changed_kinds[count++] = shared_kinds[cell];
            }
        }
        if (count > 0)
            supplies.applyShared(changed, changed_kinds, count);
    }
}
