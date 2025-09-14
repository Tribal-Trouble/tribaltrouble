package com.oddlabs.tt.mapio;

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

    public LoadedMapGenerator(WorldGenerator fallback, File mapFile) {
        this.fallback = fallback;
        this.mapFile = mapFile;
    }

    @Override
    public int getTerrainType() { return fallback.getTerrainType(); }

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

        // Merge: copy references from base, then override fields with loaded data
        float[][] heightmap = lm.heights;

        boolean[][] access_grid = base.access_grid;
        boolean[][] dock_grid = base.dock_grid;
        boolean[][] water_grid = base.water_grid;
        byte[][] build_grid = base.build_grid;
        if (lm.access != null && lm.access.length == n) access_grid = lm.access;
        if (lm.dock != null && lm.dock.length == n) dock_grid = lm.dock;
        if (lm.water != null && lm.water.length == n) water_grid = lm.water;
        if (lm.build != null && lm.build.length == n) build_grid = lm.build;

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
                build_grid,
                adjustedIslands,
                adjustedStarts);
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

    // Move each starting location to the nearest accessible/dockable tile center if blocked
    private static float[][] adjustStartingLocations(float[][] starts,
                                                     boolean[][] access,
                                                     boolean[][] dock) {
        if (starts == null) return null;
        int n = access != null ? access.length : 0;
        float[][] out = new float[starts.length][2];
        for (int i = 0; i < starts.length; i++) {
            float sx = starts[i][0];
            float sy = starts[i][1];
            int gx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(sx);
            int gy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(sy);
            gx = clamp(gx, 0, n - 1);
            gy = clamp(gy, 0, n - 1);
            if (isFree(gx, gy, access, dock)) {
                out[i][0] = sx; out[i][1] = sy;
            } else {
                int[] free = findNearestFree(gx, gy, access, dock);
                out[i][0] = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(free[0]);
                out[i][1] = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(free[1]);
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
