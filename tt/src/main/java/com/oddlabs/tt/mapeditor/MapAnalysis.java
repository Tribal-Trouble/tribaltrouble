package com.oddlabs.tt.mapeditor;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The island as it stood at one moment, measured for the editor's overlays: how well each spot is supplied, where
 * each building can stand, and how defensible each spot is. It is taken on the editor's thread by {@link #capture}
 * and can then be measured on another, so measuring a large island does not stall the editor. Each measure is worked
 * out when first asked for.
 *
 * <p>Measures are per height map cell, in rows. Scores run from 0 to 1 and are only meaningful on the playable region.
 */
final class MapAnalysis {
    /** What peons gather, with trees and palms both as wood. */
    enum Supply {
        WOOD(1f),
        ROCK(.5f),
        IRON(1f);

        /** How much it counts in the blend of all supplies; rock is the least sought after. */
        final float weight;

        Supply(float weight) {
            this.weight = weight;
        }

        boolean includes(@Nullable Resource resource) {
            return resource != null && switch (this) {
                case WOOD -> resource.isTree();
                case ROCK -> resource == Resource.ROCK;
                case IRON -> resource == Resource.IRON;
            };
        }
    }

    /** The resource on each cell, as {@link ResourceLayer#get} gives it. */
    @FunctionalInterface
    interface Resources {
        @Nullable
        Resource get(int x, int y);
    }

    /** Cells of each box blur pass; three passes spread a resource over roughly 20 cells (40 m) either way. */
    private static final int ABUNDANCE_BOX_RADIUS = 10;
    /** Cells (80 m) from the nearest resource at which a spot stops counting as close to one. */
    private static final float NEAR_REACH = 40f;
    /** How much closeness to the nearest resource counts against how many are around. */
    private static final float NEAR_WEIGHT = .4f;
    /**
     * Scores are scaled to this fraction of the playable region's spots, so one outlier does not wash out the rest.
     */
    private static final float TOP_FRACTION = .98f;

    /** The cells between tower centres two towers need: each blocks the 5 by 5 square the next one checks. */
    private static final int TOWER_SPACING = RacesResources.TOWER_SIZE + 1;
    /** Radii, in cells, of the disks passages are measured with; the widest counts as open ground. */
    private static final int @NonNull [] PASSAGE_RADII = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32};
    /**
     * Towers in range per cell of width that counts as fully defensible. Narrow ways are rare, so this is a fixed
     * scale rather than one set by the map's best spots: open ground comes to about 0.6, a 5 cell pass whose mouth
     * opens onto plenty of tower ground to about 2.5.
     */
    private static final float DEFENSE_FULL = 3f;

    /** Ship.OCCUPY_LENGTH_CELLS and Ship.OCCUPY_WIDTH_CELLS. */
    private static final int SHIP_LENGTH_CELLS = 14;
    private static final int SHIP_WIDTH_CELLS = 6;

    /** A dock band cell where a ship cannot be laid down. */
    static final byte DOCK_BLOCKED = 1;
    /** A cell a ship can be laid down from. */
    static final byte DOCK_OPEN = 2;

    private static final float DIAGONAL = (float) Math.sqrt(2);

    private final int size;
    private final AccessMap.@NonNull Kind @NonNull [] kinds;
    private final boolean @NonNull [] region;
    private final @Nullable Resource @NonNull [] resources;
    private final boolean @NonNull [] below_sea;

    private final Map<Supply, float[]> fairness = new EnumMap<>(Supply.class);
    private float @Nullable [] fairness_all;
    private final Map<Integer, boolean[]> buildable = new HashMap<>();
    private float @Nullable [] defense;
    private byte @Nullable [] docks;

    private MapAnalysis(int size, AccessMap.@NonNull Kind @NonNull [] kinds,
            @Nullable Resource @NonNull [] resources, boolean @NonNull [] below_sea) {
        this.size = size;
        this.kinds = kinds;
        this.resources = resources;
        this.below_sea = below_sea;
        this.region = new boolean[kinds.length];
        for (int i = 0; i < kinds.length; i++)
            region[i] = kinds[i] == AccessMap.Kind.REGION;
    }

    /**
     * Takes the island as it is now.
     *
     * @param heights      the height map in meters, as the terrain editor keeps it
     * @param height_scale the meters a generator height of 1 comes to
     */
    static @NonNull MapAnalysis capture(float @NonNull [] @NonNull [] heights, @NonNull AccessMap access,
            @NonNull Resources resources, float height_scale) {
        int size = access.getSize();
        AccessMap.Kind[] kinds = new AccessMap.Kind[size * size];
        Resource[] found = new Resource[size * size];
        boolean[] below_sea = new boolean[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                kinds[i] = access.get(x, y);
                found[i] = resources.get(x, y);
                below_sea[i] = heights[y][x] / height_scale <= Globals.SEA_LEVEL;
            }
        }
        return new MapAnalysis(size, kinds, found, below_sea);
    }

    int getSize() {
        return size;
    }

    /** How the playable area sorted a cell. */
    AccessMap.@NonNull Kind kind(int i) {
        return kinds[i];
    }

    boolean isRegion(int i) {
        return region[i];
    }

    private @Nullable Resource resource(int i) {
        return resources[i];
    }

    // ---- Supplies ----

    /**
     * How well each spot is supplied with one kind: how close the nearest one is, and how many there are around
     * it, the nearer the more they count. Abundance is measured against the best supplied spots on the map.
     */
    float @NonNull [] fairness(@NonNull Supply supply) {
        float[] scores = fairness.get(supply);
        if (scores != null)
            return scores;
        int n = size * size;
        float[] abundance = new float[n];
        float[] nearest = new float[n];
        for (int i = 0; i < n; i++) {
            boolean here = supply.includes(resource(i));
            abundance[i] = here ? 1f : 0f;
            nearest[i] = here ? 0f : Float.POSITIVE_INFINITY;
        }
        for (int pass = 0; pass < 3; pass++)
            boxBlur(abundance, ABUNDANCE_BOX_RADIUS);
        chamfer(nearest);
        float top = topOfRegion(abundance);
        scores = new float[n];
        for (int i = 0; i < n; i++) {
            if (!isRegion(i))
                continue;
            float near = Math.max(0f, 1f - nearest[i] / NEAR_REACH);
            float plenty = top > 0f ? Math.min(1f, abundance[i] / top) : 0f;
            scores[i] = NEAR_WEIGHT * near + (1f - NEAR_WEIGHT) * plenty;
        }
        fairness.put(supply, scores);
        return scores;
    }

    /** Every supply's fairness together, each as much as its weight. */
    float @NonNull [] fairnessAll() {
        float[] scores = fairness_all;
        if (scores != null)
            return scores;
        scores = new float[size * size];
        float total = 0f;
        for (Supply supply : Supply.values()) {
            float[] part = fairness(supply);
            for (int i = 0; i < scores.length; i++)
                scores[i] += supply.weight * part[i];
            total += supply.weight;
        }
        for (int i = 0; i < scores.length; i++)
            scores[i] /= total;
        fairness_all = scores;
        return scores;
    }

    // ---- Buildings ----

    /**
     * The cells a building of a placing size can be centred on, as LandBuilding.isPlacingLegal decides it: the
     * square reaching size - 1 cells from the centre lies on the playable region, clear of resources. (Ground off
     * the region is held by the world's unreachable marker, and the build grid's slope rule is the region's own.)
     */
    boolean @NonNull [] buildable(int placing_size) {
        boolean[] legal = buildable.get(placing_size);
        if (legal != null)
            return legal;
        // Blocked cells summed over every rectangle from the corner, to count them in any square at once.
        int stride = size + 1;
        int[] blocked = new int[stride * stride];
        for (int y = 0; y < size; y++) {
            int row = 0;
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                if (!isRegion(i) || resource(i) != null)
                    row++;
                blocked[(y + 1) * stride + x + 1] = blocked[y * stride + x + 1] + row;
            }
        }
        int reach = placing_size - 1;
        legal = new boolean[size * size];
        for (int y = reach; y < size - reach; y++) {
            for (int x = reach; x < size - reach; x++) {
                int x0 = x - reach;
                int y0 = y - reach;
                int x1 = x + reach + 1;
                int y1 = y + reach + 1;
                legal[y * size + x] = blocked[y1 * stride + x1] - blocked[y0 * stride + x1] - blocked[y1 * stride + x0] + blocked[y0 * stride + x0] == 0;
            }
        }
        buildable.put(placing_size, legal);
        return legal;
    }

    /**
     * Where ships can be built: the generator's dock band along the open sea, as {@link #DOCK_OPEN} where
     * Ship.isPlacingLegal lets a ship be laid down, its hull turned to the water and clear of resources, and as
     * {@link #DOCK_BLOCKED} elsewhere on the band.
     */
    byte @NonNull [] docks() {
        byte[] result = docks;
        if (result != null)
            return result;
        boolean[] water = sea();
        // Landscape.generateWaterGrid's dock map.
        Channel shore = toChannel(water).smooth(2).threshold(.4f, .6f);
        Channel band = toChannel(water).smooth(6).threshold(0f, .99f).channelMultiply(shore);
        result = new byte[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (band.getPixel(x, y) <= .5f)
                    continue;
                int i = y * size + x;
                result[i] = isRegion(i) && shipFits(water, x, y) ? DOCK_OPEN : DOCK_BLOCKED;
            }
        }
        docks = result;
        return result;
    }

    /** Ship.doIsPlacingLegal's hull check: no resource under the hull turned to the water. */
    private boolean shipFits(boolean @NonNull [] water, int grid_x, int grid_y) {
        float[] dir = shipDirection(water, grid_x, grid_y);
        float center_x = HeightMap.METERS_PER_UNIT_GRID * grid_x;
        float center_y = HeightMap.METERS_PER_UNIT_GRID * grid_y;
        float half_length = SHIP_LENGTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * .5f;
        float half_width = SHIP_WIDTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * .5f;
        int radius = (int) Math.ceil(Math.hypot(half_length, half_width) / HeightMap.METERS_PER_UNIT_GRID) + 1;
        for (int y = Math.max(0, grid_y - radius); y <= Math.min(size - 1, grid_y + radius); y++) {
            for (int x = Math.max(0, grid_x - radius); x <= Math.min(size - 1, grid_x + radius); x++) {
                float rel_x = UnitGrid.coordinateFromGrid(x) - center_x;
                float rel_y = UnitGrid.coordinateFromGrid(y) - center_y;
                float along = rel_x * dir[0] + rel_y * dir[1];
                float side = -rel_x * dir[1] + rel_y * dir[0];
                if (Math.abs(along) <= half_length && Math.abs(side) <= half_width
                        && resource(y * size + x) != null)
                    return false;
            }
        }
        return true;
    }

    /** Ship.getInitDirection: of 20 headings, the one that leaves the least land on one side and the most water. */
    private float @NonNull [] shipDirection(boolean @NonNull [] water, int x, int y) {
        int samples = 20;
        int best_gap = 0;
        double best_dx = 0;
        double best_dy = 0;
        double delta = Math.toRadians(360.0 / samples);
        for (int i = 0; i < samples; i++) {
            double cos = Math.cos(delta * i);
            double sin = Math.sin(delta * i);
            int weight_a = 0;
            int weight_b = 0;
            for (int ty = y - 8; ty < y + 8; ty++) {
                for (int tx = x - 8; tx < x + 8; tx++) {
                    boolean wet = tx < 0 || ty < 0 || tx >= size || ty >= size || water[ty * size + tx];
                    int land = wet ? 0 : 1;
                    if ((tx - x) * sin - (ty - y) * cos > 0)
                        weight_a += land;
                    else
                        weight_b += land;
                }
            }
            int gap = weight_b - weight_a;
            if (i == 0 || gap < best_gap) {
                best_gap = gap;
                best_dx = cos;
                best_dy = sin;
            }
        }
        return new float[]{(float) best_dy, (float) -best_dx};
    }

    /**
     * The open sea as Landscape.generateWaterGrid finds it: ground below sea level joined to the map corner, which
     * is always sea floor.
     */
    private boolean @NonNull [] sea() {
        boolean[] water = new boolean[size * size];
        int[] queue = new int[size * size];
        int head = 0;
        int tail = 0;
        if (below(0)) {
            water[0] = true;
            queue[tail++] = 0;
        }
        while (head < tail) {
            int i = queue[head++];
            int x = i % size;
            int y = i / size;
            if (x > 0)
                tail = flood(water, queue, i - 1, tail);
            if (x < size - 1)
                tail = flood(water, queue, i + 1, tail);
            if (y > 0)
                tail = flood(water, queue, i - size, tail);
            if (y < size - 1)
                tail = flood(water, queue, i + size, tail);
        }
        return water;
    }

    private boolean below(int i) {
        return below_sea[i];
    }

    private int flood(boolean @NonNull [] water, int @NonNull [] queue, int i, int tail) {
        if (!water[i] && below(i)) {
            water[i] = true;
            queue[tail++] = i;
        }
        return tail;
    }

    private @NonNull Channel toChannel(boolean @NonNull [] cells) {
        Channel channel = new Channel(size, size);
        float[][] pixels = channel.getPixels();
        for (int i = 0; i < cells.length; i++)
            pixels[i / size][i % size] = cells[i] ? 1f : 0f;
        return channel;
    }

    // ---- Defense ----

    /**
     * How defensible each spot is: the towers that could be built within a tower's range of it, for each cell of
     * width of the way through it, so narrow ways overlooked by plenty of tower ground score highest.
     */
    float @NonNull [] defense() {
        float[] scores = defense;
        if (scores != null)
            return scores;
        int n = size * size;
        float[] towers = towersInRange();
        float[] width = passageWidth();
        scores = new float[n];
        for (int i = 0; i < n; i++)
            if (width[i] > 0f)
                scores[i] = Math.min(1f, towers[i] / width[i] / DEFENSE_FULL);
        defense = scores;
        return scores;
    }

    /**
     * About how many towers could stand within range of each cell: the legal tower centres in range, over the
     * centres one tower takes from its neighbours.
     */
    private float @NonNull [] towersInRange() {
        boolean[] legal = buildable(RacesResources.TOWER_SIZE);
        // Legal centres summed along each row, to count any run of a row at once.
        int stride = size + 1;
        int[] rows = new int[size * stride];
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                rows[y * stride + x + 1] = rows[y * stride + x] + (legal[y * size + x] ? 1 : 0);
        int range = AttackScanFilter.TOWER_RANGE;
        int[] half = new int[2 * range + 1];
        for (int dy = -range; dy <= range; dy++)
            half[dy + range] = (int) Math.sqrt(range * range - dy * dy);
        float per_tower = TOWER_SPACING * TOWER_SPACING;
        float[] towers = new float[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!isRegion(y * size + x))
                    continue;
                int count = 0;
                for (int dy = -range; dy <= range; dy++) {
                    int ry = y + dy;
                    if (ry < 0 || ry >= size)
                        continue;
                    int x0 = Math.max(0, x - half[dy + range]);
                    int x1 = Math.min(size, x + half[dy + range] + 1);
                    count += rows[ry * stride + x1] - rows[ry * stride + x0];
                }
                towers[y * size + x] = count / per_tower;
            }
        }
        return towers;
    }

    /**
     * The width in cells of the way through each cell of the playable region: the widest disk on the region that
     * covers it. Resources are not in the way, as they always stand apart with ground to walk between them. Ground
     * wider than the widest disk measured counts as that wide; cells off the region are 0.
     */
    private float @NonNull [] passageWidth() {
        int n = size * size;
        boolean[] walkable = new boolean[n];
        float[] clearance = new float[n];
        for (int i = 0; i < n; i++) {
            walkable[i] = isRegion(i);
            clearance[i] = walkable[i] ? Float.POSITIVE_INFINITY : 0f;
        }
        // The map edge is never walkable, so every cell is a finite way from a blocked one.
        chamfer(clearance);
        float[] width = new float[n];
        float[] reach = new float[n];
        for (int r : PASSAGE_RADII) {
            // Centres of disks of radius r that fit, then every cell such a disk covers.
            for (int i = 0; i < n; i++)
                reach[i] = clearance[i] >= r ? 0f : Float.POSITIVE_INFINITY;
            chamfer(reach);
            for (int i = 0; i < n; i++)
                if (walkable[i] && reach[i] < r)
                    width[i] = 2 * r - 1;
        }
        return width;
    }

    // ---- Helpers ----

    /** The value that {@link #TOP_FRACTION} of the playable region's cells reach at most. */
    private float topOfRegion(float @NonNull [] values) {
        float max = 0f;
        int count = 0;
        for (int i = 0; i < values.length; i++) {
            if (isRegion(i)) {
                max = Math.max(max, values[i]);
                count++;
            }
        }
        if (max <= 0f)
            return 0f;
        int bins = 1024;
        int[] histogram = new int[bins];
        for (int i = 0; i < values.length; i++)
            if (isRegion(i))
                histogram[Math.min(bins - 1, (int) (values[i] / max * bins))]++;
        int wanted = (int) Math.ceil(count * TOP_FRACTION);
        int seen = 0;
        for (int b = 0; b < bins; b++) {
            seen += histogram[b];
            if (seen >= wanted)
                return Math.max(max / bins, (b + 1) * max / bins);
        }
        return max;
    }

    /** Averages each cell over a square reaching r cells either way, in two passes; off the map counts as 0. */
    private void boxBlur(float @NonNull [] values, int r) {
        float scale = 1f / (2 * r + 1);
        int last = Math.min(r, size - 1);
        // Along each row.
        float[] line = new float[size];
        for (int y = 0; y < size; y++) {
            int row = y * size;
            System.arraycopy(values, row, line, 0, size);
            float sum = 0f;
            for (int k = 0; k <= last; k++)
                sum += line[k];
            for (int x = 0; x < size; x++) {
                values[row + x] = sum * scale;
                if (x + r + 1 < size)
                    sum += line[x + r + 1];
                if (x - r >= 0)
                    sum -= line[x - r];
            }
        }
        // Down each column, a row at a time so memory is read in order.
        float[] sums = new float[size];
        float[] down = new float[size * size];
        for (int k = 0; k <= last; k++)
            for (int x = 0; x < size; x++)
                sums[x] += values[k * size + x];
        for (int y = 0; y < size; y++) {
            int row = y * size;
            for (int x = 0; x < size; x++)
                down[row + x] = sums[x] * scale;
            if (y + r + 1 < size)
                for (int x = 0; x < size; x++)
                    sums[x] += values[(y + r + 1) * size + x];
            if (y - r >= 0)
                for (int x = 0; x < size; x++)
                    sums[x] -= values[(y - r) * size + x];
        }
        System.arraycopy(down, 0, values, 0, values.length);
    }

    /**
     * Turns 0 cells and infinite ones into each cell's distance in cells to the nearest 0 one, near enough, by
     * steps of one cell straight or diagonally.
     */
    private void chamfer(float @NonNull [] dist) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                float d = dist[i];
                if (d == 0f)
                    continue;
                if (x > 0)
                    d = Math.min(d, dist[i - 1] + 1f);
                if (y > 0) {
                    d = Math.min(d, dist[i - size] + 1f);
                    if (x > 0)
                        d = Math.min(d, dist[i - size - 1] + DIAGONAL);
                    if (x < size - 1)
                        d = Math.min(d, dist[i - size + 1] + DIAGONAL);
                }
                dist[i] = d;
            }
        }
        for (int y = size - 1; y >= 0; y--) {
            for (int x = size - 1; x >= 0; x--) {
                int i = y * size + x;
                float d = dist[i];
                if (d == 0f)
                    continue;
                if (x < size - 1)
                    d = Math.min(d, dist[i + 1] + 1f);
                if (y < size - 1) {
                    d = Math.min(d, dist[i + size] + 1f);
                    if (x < size - 1)
                        d = Math.min(d, dist[i + size + 1] + DIAGONAL);
                    if (x > 0)
                        d = Math.min(d, dist[i + size - 1] + DIAGONAL);
                }
                dist[i] = d;
            }
        }
    }
}
