package com.oddlabs.tt.pathfinder;

public interface ScanFilter {
    int getMinRadius();

    int getMaxRadius();

    /**
     * Scan and filter using the provided occupant
     *
     * @return true if scan should stop otherwise true to continue scan
     */
    boolean filter(int grid_x, int grid_y, Occupant occ);
}
