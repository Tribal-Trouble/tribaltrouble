package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.landscape.HeightMap;

/** Runtime adapter implementing {@link PlacementGridReader}. */
public final class RuntimeGridReader implements PlacementGridReader {
    private final UnitGrid unitGrid;
    private final HeightMap heightMap;

    public RuntimeGridReader(UnitGrid ug) {
        this.unitGrid = ug;
        this.heightMap = ug.getHeightMap();
    }

    public int getGridSize() { return unitGrid.getGridSize(); }

    public boolean canBuildCenter(int gx, int gy, int sizeVal) { return heightMap.canBuild(gx, gy, sizeVal); }

    public boolean canDock(int gx, int gy) { return heightMap.canDock(gx, gy); }

    public boolean isOccupied(int gx, int gy) { return unitGrid.isGridOccupied(gx, gy, UnitGrid.LAND); }
}
