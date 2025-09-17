package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.HeightMap;

/** Editor adapter implementing {@link PlacementGridReader} reading live editor grids. */
public final class EditorGridReader implements PlacementGridReader {
    private final World world;
    private final HeightMap heightMap;

    public EditorGridReader(World w) {
        this.world = w;
        this.heightMap = w.getHeightMap();
        // Ensure occupancy grid is initialized
    com.oddlabs.tt.editor.EditorEntityOccupancy.ensure(w);
    }

    public int getGridSize() { return heightMap.getGridUnitsPerWorld(); }

    public boolean canBuildCenter(int gx, int gy, int sizeVal) { return heightMap.canBuild(gx, gy, sizeVal); }

    public boolean canDock(int gx, int gy) { return heightMap.canDock(gx, gy); }

    public boolean isOccupied(int gx, int gy) { return !com.oddlabs.tt.editor.EditorEntityOccupancy.isCellFree(world, gx, gy); }
}
