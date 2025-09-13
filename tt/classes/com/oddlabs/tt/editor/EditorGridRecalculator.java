package com.oddlabs.tt.editor;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.RacesResources;

/**
 * Recomputes gameplay grids (water, dock, access, build) from the current heightmap.
 *
 * This mirrors Landscape's worldgen logic but derives channels from the live world heights.
 * It updates the existing arrays returned by HeightMap getters in-place.
 */
public final class EditorGridRecalculator {
    private EditorGridRecalculator() {}

    public static void recomputeAll(World world, int terrainType) {
        HeightMap hm = world.getHeightMap();
        int N = hm.getGridUnitsPerWorld();
        if (N <= 0) return;

        // Normalized height channel (worldgen operates in [0..1] height units before scaling)
        float seaLevelMeters = hm.getSeaLevelMeters();
        float heightScale = seaLevelMeters / Globals.SEA_LEVEL; // meters per normalized height unit
        Channel height = new Channel(N, N);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float h = hm.getWrappedHeight(x, y) / heightScale;
                if (h < 0f) h = 0f; else if (h > 1f) h = 1f;
                height.putPixel(x, y, h);
            }
        }

        // Slope and relheight (slope used for access/build thresholds)
        Channel slope = height.copy().lineart();

        // Access/build thresholds match worldgen by map size
        float access_threshold;
        switch (hm.getMetersPerWorld()) {
            case 256: access_threshold = 0.05f; break;
            case 512: access_threshold = 0.0375f; break;
            case 1024: access_threshold = 0.025f; break;
            case 2048: access_threshold = 0.02f; break;
            default: access_threshold = 0.0375f; break;
        }
        float build_threshold = access_threshold / 2f;

        // --- Water and dock ---
        Channel water_map =
                height.copy()
                        .threshold(Globals.SEA_LEVEL - 10.0f, Globals.SEA_LEVEL)
                        .floodfill(0, 0, -1.0f, 0.1f)
                        .threshold(-1.01f, -0.99f);

        Channel dock_map =
                water_map
                        .copy()
                        .smooth(4)
                        .threshold(0.01f, 1.0f)
                        .channelMultiply(water_map.copy().invert());
        Channel beach = height.copy().threshold(Globals.SEA_LEVEL - 1.0f, Globals.SEA_LEVEL + 0.05f);
        dock_map = dock_map.channelMultiply(beach);

        boolean[][] water = hm.getWaterGrid();
        boolean[][] dock = hm.getDockGrid();
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                water[y][x] = water_map.getPixel(x, y) > 0.5f;
                dock[y][x] = dock_map.getPixel(x, y) > 0.5f;
            }
        }

        // --- Access grid ---
        Channel access = generateThresholdMap(height, slope, access_threshold, Globals.SEA_LEVEL - 0.5f)
                .channelMultiply(water_map.copy().invert());
        boolean[][] access_grid = hm.getAccessGrid();
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                access_grid[y][x] = access.getPixel(x, y) > 0f;
            }
        }

        // --- Build grid ---
        Channel buildThreshold = generateThresholdMap(height, slope, build_threshold, Globals.SEA_LEVEL);
        byte[][] build = generateBuildMap(buildThreshold);
        byte[][] build_grid = world.getHeightMap().getBuildGrid();
        for (int y = 0; y < N; y++) {
            System.arraycopy(build[y], 0, build_grid[y], 0, N);
        }
    }

    /**
     * Recompute water/dock/access/build over the full domain, but only write back
     * into the specified ROI (in grid units) expanded with a small halo to avoid
     * visible seams at chunk borders. This keeps correctness while limiting updates.
     */
    public static void recomputeROI(
            World world,
            int terrainType,
            int minGX,
            int minGY,
            int maxGX,
            int maxGY) {
        HeightMap hm = world.getHeightMap();
        int N = hm.getGridUnitsPerWorld();
        if (N <= 0) return;

        // Build normalized height and slope over entire map for correctness
        float seaLevelMeters = hm.getSeaLevelMeters();
        float heightScale = seaLevelMeters / Globals.SEA_LEVEL;
        Channel height = new Channel(N, N);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float h = hm.getWrappedHeight(x, y) / heightScale;
                if (h < 0f) h = 0f; else if (h > 1f) h = 1f;
                height.putPixel(x, y, h);
            }
        }
        Channel slope = height.copy().lineart();

        float access_threshold;
        switch (hm.getMetersPerWorld()) {
            case 256: access_threshold = 0.05f; break;
            case 512: access_threshold = 0.0375f; break;
            case 1024: access_threshold = 0.025f; break;
            case 2048: access_threshold = 0.02f; break;
            default: access_threshold = 0.0375f; break;
        }
        float build_threshold = access_threshold / 2f;

        Channel water_map =
                height.copy()
                        .threshold(Globals.SEA_LEVEL - 10.0f, Globals.SEA_LEVEL)
                        .floodfill(0, 0, -1.0f, 0.1f)
                        .threshold(-1.01f, -0.99f);
        Channel dock_map =
                water_map
                        .copy()
                        .smooth(4)
                        .threshold(0.01f, 1.0f)
                        .channelMultiply(water_map.copy().invert());
        Channel beach = height.copy().threshold(Globals.SEA_LEVEL - 1.0f, Globals.SEA_LEVEL + 0.05f);
        dock_map = dock_map.channelMultiply(beach);

        Channel access = generateThresholdMap(height, slope, access_threshold, Globals.SEA_LEVEL - 0.5f)
                .channelMultiply(water_map.copy().invert());
        Channel buildThreshold = generateThresholdMap(height, slope, build_threshold, Globals.SEA_LEVEL);
        byte[][] build = generateBuildMap(buildThreshold);

        // Write back only inside ROI + halo
        final int HALO = 8; // small cushion in grid units
        int x0 = minGX - HALO, y0 = minGY - HALO, x1 = maxGX + HALO, y1 = maxGY + HALO;
        boolean[][] water = hm.getWaterGrid();
        boolean[][] dock = hm.getDockGrid();
        boolean[][] access_grid = hm.getAccessGrid();
        byte[][] build_grid = hm.getBuildGrid();

        for (int gy = y0; gy <= y1; gy++) {
            int wy = wrap(gy, N);
            for (int gx = x0; gx <= x1; gx++) {
                int wx = wrap(gx, N);
                water[wy][wx] = water_map.getPixel(wx, wy) > 0.5f;
                dock[wy][wx] = dock_map.getPixel(wx, wy) > 0.5f;
                access_grid[wy][wx] = access.getPixel(wx, wy) > 0f;
                build_grid[wy][wx] = build[wy][wx];
            }
        }
    }

    private static int wrap(int v, int N) { return (v % N + N) % N; }

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
            for (int x = 0; x < size; x++) {
                build_grid[y][x] = thresholdmap.getPixel(x, y) > 0f;
            }
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
}
