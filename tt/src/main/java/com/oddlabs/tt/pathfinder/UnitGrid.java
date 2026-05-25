package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.util.Target;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class UnitGrid {
    private final @NonNull HeightMap heightmap;

    public static final int LAND = 0;
    public static final int SEA = 1;
    public static final int NUM_LAYERS = 2;

    static class Layer {
        final @NonNull Region @NonNull [] @NonNull [] regions;
        final @Nullable Occupant @NonNull [] @NonNull [] occupants;

        Layer(int size) {
            occupants = new Occupant[size][size];
            regions = new Region[size][size];
        }
    }

    private final @NonNull Layer[] layers;

    public UnitGrid(@NonNull HeightMap heightmap) {
        this.heightmap = heightmap;
        int unit_grid_size = heightmap.getAccessGrid().length;
        layers = new Layer[2];
        layers[LAND] = new Layer(unit_grid_size);
        layers[SEA] = new Layer(unit_grid_size);
    }

    private boolean filter(@NonNull ScanFilter filter, int x, int y) {
        return filter(filter, x, y, LAND);
    }

    private boolean filter(@NonNull ScanFilter filter, int x, int y, int layer) {
        Occupant[][] occupants = layers[layer].occupants;
        if (x < 0 || y < 0 || x >= occupants.length || y >= occupants.length)
            return false;
        return filter.filter(x, y, occupants[y][x]);
    }

    public Target @NonNull [] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only) {
        return findGridTargets(center_grid_x, center_grid_y, num_targets, grid_targets_only, LAND);
    }

    public Target @NonNull [] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only, int layer) {
        Occupant[][] occupants = layers[layer].occupants;
        FindTargetsFilter filter = new FindTargetsFilter(num_targets, occupants.length, grid_targets_only);
        scan(filter, center_grid_x, center_grid_y, layer);
        return filter.getTargets();
    }

    public void scan(@NonNull ScanFilter filter, int center_grid_x, int center_grid_y) {
        scan(filter, center_grid_x, center_grid_y, LAND);
    }

    public void scan(@NonNull ScanFilter filter, int center_grid_x, int center_grid_y, int layer) {
        int radius = filter.getMinRadius();
        if (radius == 0) {
            if (filter(filter, center_grid_x, center_grid_y, layer))
                return;
            radius++;
        }
        while (radius <= filter.getMaxRadius()) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (filter(filter, x, y_i, layer))
                    return;
                if (filter(filter, x2, y_i, layer))
                    return;
            }
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            for (int i = 0; i < 2 * radius + 1; i++) {
                int x_i = center_grid_x - radius + i;
                if (filter(filter, x_i, y, layer))
                    return;
                if (filter(filter, x_i, y2, layer))
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
        return layers[LAND].occupants.length;
    }

    public Region getRegion(int grid_x, int grid_y) {
        return getRegion(grid_x, grid_y, LAND);
    }

    public Region getRegion(int grid_x, int grid_y, int layer) {
        Region region = layers[layer].regions[grid_y][grid_x];
        return region;
    }

    public void setRegion(int grid_x, int grid_y, Region r) {
        setRegion(grid_x, grid_y, r, LAND);
    }

    public void setRegion(int grid_x, int grid_y, Region r, int layer) {
        assert layers[layer].regions[grid_y][grid_x] == null && !isGridOccupied(grid_x, grid_y, layer);
        layers[layer].regions[grid_y][grid_x] = r;
    }

    public boolean isGridOccupied(int grid_x, int grid_y) {
        return isGridOccupied(grid_x, grid_y, LAND);
    }

    public boolean isGridOccupied(int grid_x, int grid_y, int layer) {
        return layers[layer].occupants[grid_y][grid_x] != null;
    }

    public @Nullable Occupant getOccupant(int grid_x, int grid_y) {
        return getOccupant(grid_x, grid_y, LAND);
    }

    public @Nullable Occupant getOccupant(int grid_x, int grid_y, int layer) {
        return layers[layer].occupants[grid_y][grid_x];
    }

    public void occupyGrid(int grid_x, int grid_y, Occupant occupant) {
        occupyGrid(grid_x, grid_y, occupant, LAND);
    }

    public void occupyGrid(int grid_x, int grid_y, Occupant occupant, int layer) {
        assert !isGridOccupied(grid_x, grid_y, layer);
        layers[layer].occupants[grid_y][grid_x] = occupant;
    }

    public final boolean isWater(int grid_x, int grid_y) {
        return heightmap.getWaterGrid()[grid_y][grid_x];
    }

    public final boolean isDockable(int grid_x, int grid_y) {
        return heightmap.getDockGrid()[grid_y][grid_x];
    }

    public void freeGrid(int grid_x, int grid_y, Occupant occupant) {
        freeGrid(grid_x, grid_y, occupant, LAND);
    }

    public void freeGrid(int grid_x, int grid_y, Occupant occupant, int layer) {
        assert layers[layer].occupants[grid_y][grid_x] == occupant : occupant + " trying to free " + grid_x + " " + grid_y + " where " + layers[layer].occupants[grid_y][grid_x] + " is.";
        layers[layer].occupants[grid_y][grid_x] = null;
    }

    public void debugRenderRegions(float landscape_x, float landscape_y) {
        int RADIUS = 30;
        int center_x = toGridCoordinate(landscape_x);
        int center_y = toGridCoordinate(landscape_y);
        int start_x = Math.max(0, center_x - RADIUS);
        int end_x = Math.min(layers[LAND].occupants.length - 0, center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(layers[LAND].occupants.length - 0, center_y + RADIUS);
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
        int end_x = Math.min(layers[LAND].occupants.length - 0, center_x + RADIUS);
        int start_y = Math.max(0, center_y - RADIUS);
        int end_y = Math.min(layers[LAND].occupants.length - 0, center_y + RADIUS);
        for (int y = start_y; y < end_y; y++) {
            for (int x = start_x; x < end_x; x++) {
                if (isGridOccupied(x, y)) {
                    debugRenderQuad(x, y);
                }
            }
        }
    }
}
