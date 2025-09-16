package com.oddlabs.tt.mapio;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * WorldGenerator wrapper that loads a .ttmap and overlays its data on top of a fallback generator.
 *
 * Strategy:
 * - Delegate to the fallback to get all render assets (colormaps, detail, structures, trees, etc.).
 * - Replace heightmap and gameplay grids when the loaded map matches the base size.
 * - Replace rock/iron/plants with those from the map; keep trees/palms from the fallback.
 * - If the map fails to load or sizes mismatch, the fallback world is returned unchanged.
 */
public final class LoadedMapGenerator implements WorldGenerator {
    private static final long serialVersionUID = 1L;

    private final WorldGenerator fallback;
    private final File mapFile;
    // If available from META, prefer the map's terrain type for visuals
    private final Integer terrainTypeOverride;

    public LoadedMapGenerator(WorldGenerator fallback, File mapFile) {
        this.fallback = fallback;
        this.mapFile = mapFile;
        Integer terr = null;
        try {
            MapIO.MapSummary sum = MapIO.peek(mapFile);
            terr = sum != null ? sum.terrainType : null;
        } catch (Throwable ignore) {}
        this.terrainTypeOverride = terr;
    }

    @Override
    public int getTerrainType() {
        if (terrainTypeOverride != null && terrainTypeOverride.intValue() >= 0) {
            return terrainTypeOverride.intValue();
        }
        return fallback.getTerrainType();
    }

    @Override
    public int getMetersPerWorld() { return fallback.getMetersPerWorld(); }

    @Override
    @SuppressWarnings("unchecked")
    public WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos) {
        WorldInfo base = fallback.generate(num_players, initial_unit_count, random_start_pos);
        MapIO.LoadedMap lm;
        try {
            lm = MapIO.load(mapFile);
        } catch (IOException e) {
            System.err.println("[LoadedMapGenerator] Failed to load .ttmap: " + e.getMessage());
            return base;
        }

        if (lm.heights == null) return base;
        int n = base.heightmap != null ? base.heightmap.length : 0;
        if (n == 0 || lm.heights.length != n || lm.heights[0].length != n) {
            System.err.println(
                    "[LoadedMapGenerator] Map size mismatch (map="
                            + (lm.heights != null ? lm.heights.length : -1)
                            + ", base="
                            + n
                            + ") — using fallback world.");
            return base;
        }

        // Merge: always take heights from the map, then derive gameplay grids from the map
        // (ignore Terrain menu/base-generator grid parameters). If the map provides a grid, use it;
        // otherwise compute it from heights + sea level using the same thresholds as worldgen.
        float[][] heightmap = lm.heights;

        // Determine sea level to normalize heights (meters -> normalized where Globals.SEA_LEVEL)
        float seaLevelMeters = (lm.seaLevel != 0f ? lm.seaLevel : base.sea_level_meters);
        float heightScale = seaLevelMeters / Globals.SEA_LEVEL;

        // Build normalized height channel [0..1]
        Channel height = new Channel(n, n);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float h = heightmap[y][x] / heightScale;
                if (h < 0f) h = 0f; else if (h > 1f) h = 1f;
                height.putPixel(x, y, h);
            }
        }

        // Compute helper maps
        Channel slope = height.copy().lineart();
    Channel water_map =
        height.copy()
            .threshold(Globals.SEA_LEVEL - 10.0f, Globals.SEA_LEVEL)
            .floodfill(0, 0, -1.0f, 0.1f, new int[1])
            .threshold(-1.01f, -0.99f);
        Channel dock_map =
                water_map.copy()
                        .smooth(4)
                        .threshold(0.01f, 1.0f)
                        .channelMultiply(water_map.copy().invert());
        Channel beach = height.copy().threshold(Globals.SEA_LEVEL - 1.0f, Globals.SEA_LEVEL + 0.05f);
        dock_map = dock_map.channelMultiply(beach);

        // Access/build thresholds depend on world size (match Landscape/EditorGridRecalculator)
        float access_threshold;
        switch (base.meters_per_world) {
            case 256:
                access_threshold = 0.05f;
                break;
            case 512:
                access_threshold = 0.0375f;
                break;
            case 1024:
                access_threshold = 0.025f;
                break;
            case 2048:
                access_threshold = 0.02f;
                break;
            default:
                access_threshold = 0.0375f;
                break;
        }
        float build_threshold = access_threshold / 2f;

        Channel access_map = generateThresholdMap(height, slope, access_threshold, Globals.SEA_LEVEL - 0.5f)
                .channelMultiply(water_map.copy().invert());
        Channel buildThreshold = generateThresholdMap(height, slope, build_threshold, Globals.SEA_LEVEL);
        byte[][] computed_build = generateBuildMap(buildThreshold);

        // Convert Channels to boolean grids
        boolean[][] computed_water = new boolean[n][n];
        boolean[][] computed_dock = new boolean[n][n];
        boolean[][] computed_access = new boolean[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                computed_water[y][x] = water_map.getPixel(x, y) > 0.5f;
                computed_dock[y][x] = dock_map.getPixel(x, y) > 0.5f;
                computed_access[y][x] = access_map.getPixel(x, y) > 0f;
            }
        }

        // Prefer map-provided grids when available; otherwise use the computed ones
        boolean[][] access_grid = (lm.access != null && lm.access.length == n) ? lm.access : computed_access;
        boolean[][] dock_grid = (lm.dock != null && lm.dock.length == n) ? lm.dock : computed_dock;
        boolean[][] water_grid = (lm.water != null && lm.water.length == n) ? lm.water : computed_water;
        byte[][] build_grid = (lm.build != null && lm.build.length == n) ? lm.build : computed_build;

        // Supplies: convert lists to the expected WorldInfo formats
    List<int[]> rock_positions = new ArrayList<>();
    for (int[] rc : lm.rocks) rock_positions.add(new int[] {rc[0], rc[1]});
    List<int[]> iron_positions = new ArrayList<>();
    for (int[] ic : lm.iron) iron_positions.add(new int[] {ic[0], ic[1]});

        // Trees: if provided, override base trees/palms by splitting types
    List<int[]> trees_positions = (List<int[]>) base.trees;
    List<int[]> palm_positions = (List<int[]>) base.palm_trees;
        if (lm.trees != null && !lm.trees.isEmpty()) {
            trees_positions = new ArrayList<>();
            palm_positions = new ArrayList<>();
            for (MapIO.Tree t : lm.trees) {
                int ti = t.typeIndex;
                if (ti == com.oddlabs.tt.landscape.AbstractTreeGroup.TREE_INDEX
                        || ti == com.oddlabs.tt.landscape.AbstractTreeGroup.OAKTREE_INDEX) {
                    trees_positions.add(new int[] {t.gx, t.gy});
                } else if (ti == com.oddlabs.tt.landscape.AbstractTreeGroup.PALMTREE_INDEX
                        || ti == com.oddlabs.tt.landscape.AbstractTreeGroup.PINETREE_INDEX) {
                    palm_positions.add(new int[] {t.gx, t.gy});
                } else {
                    // Unknown type; ignore gracefully
                }
            }
        }

        // Plants: WorldInfo expects float[types][x0,y0,x1,y1,...]
        int types = base.plants != null ? base.plants.length : 4;
        float[][] plants = new float[types][];
        int[] counts = new int[types];
        for (MapIO.Plant p : lm.plants) {
            int t = p.typeIndex < 0 ? 0 : Math.min(p.typeIndex, types - 1);
            counts[t] += 2;
        }
        for (int t = 0; t < types; t++) plants[t] = new float[counts[t]];
        int[] idx = new int[types];
        for (MapIO.Plant p : lm.plants) {
            int t = p.typeIndex < 0 ? 0 : Math.min(p.typeIndex, types - 1);
            int i = idx[t];
            plants[t][i] = p.x; plants[t][i + 1] = p.y;
            idx[t] = i + 2;
        }

        // Ensure region seed points and starting positions remain valid relative to new grids
    java.util.ArrayList<int[]> adjustedIslands = adjustIslandLocations(base.island_locations, access_grid, dock_grid);
        float[][] adjustedStarts = adjustStartingLocations(base.starting_locations, access_grid, dock_grid);

    return new WorldInfo(
                base.meters_per_world,
                (lm.seaLevel != 0f ? lm.seaLevel : base.sea_level_meters),
                base.texels_per_colormap,
                base.chunks_per_colormap,
                base.colormaps,
                base.detail,
                base.structures,
                heightmap,
                trees_positions,
                palm_positions,
                rock_positions,
                iron_positions,
                plants,
                access_grid,
                dock_grid,
                water_grid,
        dock_map,
        build_grid,
                adjustedIslands,
                adjustedStarts);
    }

    // --- Grid generation helpers (mirrors worldgen/editor) ---
    private static Channel generateThresholdMap(Channel height, Channel slope, float threshold, float min) {
        Channel channel = slope.copy().threshold(0f, threshold).channelSubtract(height.copy().threshold(0f, min));
        int size = slope.getWidth();
        // Fix wrap edges like worldgen
        for (int y = 0; y < size; y += (size - 1)) {
            for (int x = 0; x < size; x++) channel.putPixel(x, y, 0f);
        }
        for (int y = 1; y < size - 1; y++) {
            for (int x = 0; x < size; x += (size - 1)) channel.putPixel(x, y, 0f);
        }
        return channel;
    }

    private static byte[][] generateBuildMap(Channel thresholdmap) {
        int size = thresholdmap.getWidth();
        boolean[][] build_grid = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) build_grid[y][x] = thresholdmap.getPixel(x, y) > 0f;
        }
        byte[][] byte_grid = new byte[size][size];
        byte max = (byte) Math.max(RacesResources.QUARTERS_SIZE, Math.max(RacesResources.ARMORY_SIZE, RacesResources.TOWER_SIZE));
        for (byte i = 0; i < max; i++) {
            for (int y = 1; y < size - 1; y++) {
                for (int x = 1; x < size - 1; x++) {
                    if (!build_grid[y][x] && byte_grid[y][x] == i) {
                        for (int k = -1; k <= 1; k++) {
                            for (int l = -1; l <= 1; l++) {
                                if (build_grid[y + k][x + l]) {
                                    build_grid[y + k][x + l] = false;
                                    byte_grid[y + k][x + l] = (byte) (i + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                if (build_grid[y][x]) byte_grid[y][x] = max;
            }
        }
        return byte_grid;
    }

    // Move each island seed to the nearest accessible or dockable tile if currently blocked
    private static java.util.ArrayList<int[]> adjustIslandLocations(java.util.ArrayList<int[]> islands,
                                                             boolean[][] access,
                                                             boolean[][] dock) {
        if (islands == null) return null;
        int n = access != null ? access.length : 0;
    java.util.ArrayList<int[]> out = new java.util.ArrayList<>(islands.size());
        for (Object o : islands) {
            int[] pos = (int[]) o;
            int gx = clamp(pos[0], 0, n - 1);
            int gy = clamp(pos[1], 0, n - 1);
            int[] free = findNearestFree(gx, gy, access, dock);
            out.add(new int[] {free[0], free[1]});
        }
        return out;
    }

    // Move each starting location (for every unit slot per player) to nearest free tile if blocked
    private static float[][] adjustStartingLocations(float[][] starts,
                                                     boolean[][] access,
                                                     boolean[][] dock) {
        if (starts == null) return null;
        int n = (access != null && access.length > 0) ? access.length : 0;
        float[][] out = new float[starts.length][];
        for (int p = 0; p < starts.length; p++) {
            float[] playerStarts = starts[p];
            if (playerStarts == null) { out[p] = null; continue; }
            // Preserve the number of spawn coordinates for this player
            out[p] = new float[playerStarts.length];
            // Iterate pairs (x,y)
            for (int k = 0; k + 1 < playerStarts.length; k += 2) {
                float sx = playerStarts[k];
                float sy = playerStarts[k + 1];
                if (n <= 0) { // no grids provided; keep as-is
                    out[p][k] = sx; out[p][k + 1] = sy;
                    continue;
                }
                int gx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(sx);
                int gy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(sy);
                gx = clamp(gx, 0, n - 1);
                gy = clamp(gy, 0, n - 1);
                if (isFree(gx, gy, access, dock)) {
                    out[p][k] = sx; out[p][k + 1] = sy;
                } else {
                    int[] free = findNearestFree(gx, gy, access, dock);
                    out[p][k] = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(free[0]);
                    out[p][k + 1] = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(free[1]);
                }
            }
        }
        return out;
    }

    private static boolean isFree(int gx, int gy, boolean[][] access, boolean[][] dock) {
        if (access == null || dock == null) return true;
        if (gy < 0 || gx < 0 || gy >= access.length || gx >= access.length) return false;
        return access[gy][gx] || dock[gy][gx];
    }

    private static int[] findNearestFree(int gx, int gy, boolean[][] access, boolean[][] dock) {
        int n = access.length;
        if (isFree(gx, gy, access, dock)) return new int[] {gx, gy};
        // Expand square rings like UnitGrid.scan
        int radius = 1;
        while (radius < n) {
            int x0 = gx - radius, x1 = gx + radius;
            int y0 = gy - radius, y1 = gy + radius;
            for (int x = x0; x <= x1; x++) {
                if (y0 >= 0 && y0 < n && x >= 0 && x < n && isFree(x, y0, access, dock)) return new int[] {x, y0};
                if (y1 >= 0 && y1 < n && x >= 0 && x < n && isFree(x, y1, access, dock)) return new int[] {x, y1};
            }
            for (int y = y0 + 1; y <= y1 - 1; y++) {
                if (x0 >= 0 && x0 < n && y >= 0 && y < n && isFree(x0, y, access, dock)) return new int[] {x0, y};
                if (x1 >= 0 && x1 < n && y >= 0 && y < n && isFree(x1, y, access, dock)) return new int[] {x1, y};
            }
            radius++;
        }
        // Fallback (should not happen): clamp to bounds
        return new int[] {clamp(gx, 0, n - 1), clamp(gy, 0, n - 1)};
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
