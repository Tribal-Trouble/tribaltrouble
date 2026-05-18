package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.util.Target;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class UnitGrid {
    private final @NonNull Region @NonNull [] @NonNull [] regions;
    private final @Nullable Occupant @NonNull [] @NonNull [] occupants;
    private final @NonNull HeightMap heightmap;

    public UnitGrid(@NonNull HeightMap heightmap) {
        this.heightmap = heightmap;
        int unit_grid_size = heightmap.getAccessGrid().length;
        occupants = new Occupant[unit_grid_size][unit_grid_size];
        regions = new Region[unit_grid_size][unit_grid_size];
    }

    private boolean filter(@NonNull ScanFilter filter, int x, int y) {
        if (x < 0 || y < 0 || x >= occupants.length || y >= occupants.length)
            return false;
        return filter.filter(x, y, occupants[y][x]);
    }

    public Target @NonNull [] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only) {
        FindTargetsFilter filter = new FindTargetsFilter(num_targets, occupants.length, grid_targets_only);
        scan(filter, center_grid_x, center_grid_y);
        return filter.getTargets();
    }

    public void scan(@NonNull ScanFilter filter, int center_grid_x, int center_grid_y) {
        int radius = filter.getMinRadius();
        if (radius == 0) {
            if (filter(filter, center_grid_x, center_grid_y))
                return;
            radius++;
        }
        while (radius <= filter.getMaxRadius()) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (filter(filter, x, y_i))
                    return;
                if (filter(filter, x2, y_i))
                    return;
            }
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            for (int i = 0; i < 2 * radius + 1; i++) {
                int x_i = center_grid_x - radius + i;
                if (filter(filter, x_i, y))
                    return;
                if (filter(filter, x_i, y2))
                    return;
            }
            radius++;
        }
    }

    public static float coordinateFromGrid(int g) {
        return (g + .5f) * HeightMap.METERS_PER_UNIT_GRID;
    }

    public static int toGridCoordinate(float c) {
        return (int) (c / HeightMap.METERS_PER_UNIT_GRID);
    }

    public int getGridSize() {
        return occupants.length;
    }

    public Region getRegion(int grid_x, int grid_y) {
        Region region = regions[grid_y][grid_x];
        return region;
    }

    public void setRegion(int grid_x, int grid_y, Region r) {
        assert regions[grid_y][grid_x] == null && !isGridOccupied(grid_x, grid_y);
        regions[grid_y][grid_x] = r;
    }

    public boolean isGridOccupied(int grid_x, int grid_y) {
        return occupants[grid_y][grid_x] != null;
    }

    public @Nullable Occupant getOccupant(int grid_x, int grid_y) {
        return occupants[grid_y][grid_x];
    }

    public void occupyGrid(int grid_x, int grid_y, Occupant occupant) {
        assert !isGridOccupied(grid_x, grid_y);
        occupants[grid_y][grid_x] = occupant;
    }

    public void freeGrid(int grid_x, int grid_y, Occupant occupant) {
        assert occupants[grid_y][grid_x] == occupant : occupant + " trying to free " + grid_x + " " + grid_y + " where " + occupants[grid_y][grid_x] + " is.";
        occupants[grid_y][grid_x] = null;
    }

    public void debugRenderRegions(float landscape_x, float landscape_y) {
        int RADIUS = 30;
        int center_x = toGridCoordinate(landscape_x);
        int center_y = toGridCoordinate(landscape_y);
        int start_x = Math.max(0, center_x - RADIUS);
        int end_x = Math.min(occupants.length - 0, center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(occupants.length - 0, center_y + RADIUS);
        Region last_region = null;
        for (int y = start_y; y < end_y; y++) {
            for (int x = start_x; x < end_x; x++) {
                float xf = coordinateFromGrid(x);
                float yf = coordinateFromGrid(y);
                float zf = heightmap.getNearestHeight(xf, yf) + 2f;
                Region region = getRegion(x, y);
                if (region == null) {
                    DebugRender.drawPoint(xf, yf, zf, 3f, 1f, 0f, 0f);
                } else {
                    last_region = region;
                    Vector4fc color = DebugRender.debug_colors[region.hashCode() % DebugRender.debug_colors.length];
                    DebugRender.drawPoint(xf, yf, zf, 3f, color.x(), color.y(), color.z());
                }
            }
        }
        if (last_region != null) {
            last_region.debugRenderConnections(heightmap);
            last_region.debugRenderConnectionsReset();
        }
    }

    private void debugRenderQuad(int x, int y) {
        final float OFFSET = 2f;
        final float RADIUS = .5f;
        int s = HeightMap.METERS_PER_UNIT_GRID;
        float xf = (x + .5f) * s;
        float yf = (y + .5f) * s;
        float z = heightmap.getNearestHeight(xf, yf) + OFFSET;
        DebugRender.drawLine(xf - RADIUS, yf - RADIUS, z, xf + RADIUS, yf + RADIUS, z, 1f, 1f, 0f);
        DebugRender.drawLine(xf + RADIUS, yf - RADIUS, z, xf - RADIUS, yf + RADIUS, z, 1f, 1f, 0f);
    }

    public @NonNull HeightMap getHeightMap() {
        return heightmap;
    }

    public void debugRender(float landscape_x, float landscape_y) {
        int RADIUS = 30;
        int center_x = toGridCoordinate(landscape_x);
        int center_y = toGridCoordinate(landscape_y);
        int start_x = Math.max(0, center_x - RADIUS);
        int end_x = Math.min(occupants.length - 0, center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(occupants.length - 0, center_y + RADIUS);
        for (int y = start_y; y < end_y; y++) {
            for (int x = start_x; x < end_x; x++) {
                if (isGridOccupied(x, y)) {
                    debugRenderQuad(x, y);
                }
            }
        }
    }
}
