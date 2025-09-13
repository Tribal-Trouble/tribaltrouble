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

        return new WorldInfo(
                base.meters_per_world,
                (lm.seaLevel != 0f ? lm.seaLevel : base.sea_level_meters),
                base.texels_per_colormap,
                base.chunks_per_colormap,
                base.colormaps,
                base.detail,
                base.structures,
                heightmap,
                base.trees,
                base.palm_trees,
                rock_positions,
                iron_positions,
                plants,
                access_grid,
                dock_grid,
                water_grid,
                build_grid,
                base.island_locations,
                base.starting_locations);
    }
}
