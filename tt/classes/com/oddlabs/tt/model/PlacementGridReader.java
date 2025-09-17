package com.oddlabs.tt.model;

/**
 * Minimal read-only abstraction over the grids needed for building placement legality.
 * Keeps runtime (UnitGrid + HeightMap) and editor (live HeightMap + editor occupancy) paths DRY.
 */
public interface PlacementGridReader {
    int getGridSize();
    /** Return true if center cell supports building of given size (mirrors HeightMap.canBuild). */
    boolean canBuildCenter(int gx, int gy, int sizeVal);
    boolean canDock(int gx, int gy);
    boolean isOccupied(int gx, int gy);
}
