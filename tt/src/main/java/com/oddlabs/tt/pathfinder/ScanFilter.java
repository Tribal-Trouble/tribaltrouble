package com.oddlabs.tt.pathfinder;

public interface ScanFilter {
    int getMinRadius();

    int getMaxRadius();

    boolean filter(int grid_x, int grid_y, Occupant occ);
}
