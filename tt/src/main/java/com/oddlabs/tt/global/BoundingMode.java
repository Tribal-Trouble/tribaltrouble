package com.oddlabs.tt.global;

/**
 * Debug visualization modes for rendering bounding boxes and debug overlays.
 * Cycle through modes with the D key during development.
 */
public enum BoundingMode {
    /**
     * No debug visualization
     */
    NONE,
    /**
     * Show unit grid occupation with yellow X marks and selected unit paths (red/blue/white lines)
     */
    UNIT_GRID,
    /**
     * Show landscape patch bounding boxes
     */
    LANDSCAPE,
    /**
     * Show player unit/building bounding boxes
     */
    PLAYERS,
    /**
     * Show occupied grid cells (pathfinding)
     */
    OCCUPATION,
    /**
     * Show pathfinding regions as colored points with connections
     */
    REGIONS,
    /**
     * Show all debug visualizations
     */
    ALL;

    /**
     * Returns the next bounding mode in the cycle.
     *
     * @return next mode, wrapping to NONE after ALL
     */
    public BoundingMode next() {
        int nextOrdinal = (ordinal() + 1) % values().length;
        return values()[nextOrdinal];
    }
}
