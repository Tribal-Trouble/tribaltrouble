package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.global.Globals;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Sorts the island's cells the way the generator decides where units can go, which is the region players start in
 * and supplies are placed on.
 *
 * <p>This follows {@code Landscape}: a cell is accessible when its slope (the largest height step to a neighbour) is
 * within the island's threshold, it is above sea level and it is not on the map edge. The playable region is the
 * largest group of accessible cells joined side to side ({@code Channel.largestConnected}, the first found winning
 * a tie); on an archipelago every accessible cell counts, as each island is reached by ship.
 */
final class AccessMap {
    enum Kind {
        /** Sea, or level ground on the map edge. */
        NONE,
        /** Part of the playable region. */
        REGION,
        /** Walkable, but cut off from the playable region. */
        CUT_OFF,
        /** Land too steep to walk. */
        CLIFF
    }

    private final float @NonNull [] @NonNull [] heights;
    private final int size;
    private final float height_scale;
    private final float access_threshold;
    private final boolean archipelago;

    private final @NonNull Kind @NonNull [] kinds;
    // The kinds as of the compute before the last, to tell which cells an edit took out of the playable region.
    private final @Nullable Kind @NonNull [] previous;
    private final boolean @NonNull [] accessible;
    private final boolean @NonNull [] steep;
    private final boolean @NonNull [] land;
    private final float @NonNull [] normalized;
    private final int @NonNull [] component;
    private final int @NonNull [] queue;
    private boolean stale;

    AccessMap(float @NonNull [] @NonNull [] heights, @NonNull MapSettings settings) {
        this.heights = heights;
        this.size = heights.length;
        this.height_scale = settings.getHeightScale();
        this.access_threshold = settings.getAccessThreshold();
        this.archipelago = settings.isArchipelago();
        this.kinds = new Kind[size * size];
        this.previous = new Kind[size * size];
        this.accessible = new boolean[size * size];
        this.steep = new boolean[size * size];
        this.land = new boolean[size * size];
        this.normalized = new float[size * size];
        this.component = new int[size * size];
        this.queue = new int[size * size];
        compute();
        // Nothing was lost before the first edit.
        System.arraycopy(kinds, 0, previous, 0, kinds.length);
    }

    int getSize() {
        return size;
    }

    /** The kind of the cell at (x, y), as of the last {@link #compute}. */
    @NonNull
    Kind get(int x, int y) {
        return kinds[y * size + x];
    }

    /** Whether a cell is under the sea, as of the last {@link #compute}. */
    boolean isSea(int x, int y) {
        return !land[y * size + x];
    }

    /** Whether a cell left the playable region between the compute before the last and the last. */
    boolean leftRegion(int x, int y) {
        int i = y * size + x;
        return previous[i] == Kind.REGION && kinds[i] != Kind.REGION;
    }

    /** Notes that heights changed, so the cells must be sorted again. */
    void heightsChanged() {
        stale = true;
    }

    boolean isStale() {
        return stale;
    }

    /** Sorts every cell from the current heights. */
    void compute() {
        stale = false;
        System.arraycopy(kinds, 0, previous, 0, kinds.length);
        for (int y = 0; y < size; y++) {
            float[] row = heights[y];
            for (int x = 0; x < size; x++)
                normalized[y * size + x] = row[x] / height_scale;
        }
        for (int y = 0; y < size; y++) {
            // Channel.lineart, which wraps at the map edge.
            int up = (y == 0 ? size - 1 : y - 1) * size;
            int down = (y == size - 1 ? 0 : y + 1) * size;
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                int left = x == 0 ? i + size - 1 : i - 1;
                int right = x == size - 1 ? i - size + 1 : i + 1;
                float h = normalized[i];
                float slope = Math.max(
                        Math.max(Math.abs(h - normalized[left]), Math.abs(h - normalized[right])),
                        Math.max(Math.abs(h - normalized[up + x]), Math.abs(h - normalized[down + x])));
                // Landscape.generateThresholdMap: slope within [0, threshold], height outside [0, sea level], and
                // never the map edge.
                boolean walkable = slope >= 0f && slope <= access_threshold;
                boolean below_sea = h >= 0f && h <= Globals.SEA_LEVEL;
                boolean edge = x == 0 || y == 0 || x == size - 1 || y == size - 1;
                accessible[i] = walkable && !below_sea && !edge;
                land[i] = !below_sea;
                steep[i] = !walkable;
            }
        }

        int region = archipelago ? -1 : largestComponent();
        for (int i = 0; i < kinds.length; i++) {
            if (accessible[i]) {
                kinds[i] = archipelago || component[i] == region ? Kind.REGION : Kind.CUT_OFF;
            } else if (land[i] && steep[i]) {
                kinds[i] = Kind.CLIFF;
            } else {
                kinds[i] = Kind.NONE;
            }
        }
    }

    /**
     * Labels the side-to-side joined groups of accessible cells in {@link #component}, searching in row order.
     *
     * @return the label of the largest group, the first found among equals, or -1 if there is none
     */
    private int largestComponent() {
        java.util.Arrays.fill(component, -1);
        int largest = -1;
        int largest_count = 0;
        int label = 0;
        for (int start = 0; start < accessible.length; start++) {
            if (!accessible[start] || component[start] != -1)
                continue;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            component[start] = label;
            while (head < tail) {
                int i = queue[head++];
                int x = i % size;
                int y = i / size;
                if (x > 0)
                    tail = visit(i - 1, label, tail);
                if (x < size - 1)
                    tail = visit(i + 1, label, tail);
                if (y > 0)
                    tail = visit(i - size, label, tail);
                if (y < size - 1)
                    tail = visit(i + size, label, tail);
            }
            if (tail > largest_count) {
                largest_count = tail;
                largest = label;
            }
            label++;
        }
        return largest;
    }

    private int visit(int i, int label, int tail) {
        if (accessible[i] && component[i] == -1) {
            component[i] = label;
            queue[tail++] = i;
        }
        return tail;
    }
}
