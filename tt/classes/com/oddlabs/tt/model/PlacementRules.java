package com.oddlabs.tt.model;

/** Shared pure predicates for placement legality (runtime + editor). */
public final class PlacementRules {
    private PlacementRules() {}

    /**
     * Determine if a building footprint (size, nearSea) is legal at (grid_x,grid_y).
     * Logic mirrors previous Building.doIsPlacingLegal.
     * size = template.getPlacingSize() or (placingSize-PLACING_BORDER) depending on caller semantics.
     */
    public static boolean isPlacingLegal(
            PlacementGridReader grid, int grid_x, int grid_y, int size, boolean near_sea) {
        if (!near_sea && !grid.canBuildCenter(grid_x, grid_y, size)) return false;
        if (near_sea && !grid.canDock(grid_x, grid_y)) return false;

        int effectiveSize = near_sea ? 1 : size; // footprint shrink for docks
        int gridSize = grid.getGridSize();
        int span = effectiveSize * 2 - 1;
        for (int y = 0; y < span; y++) {
            for (int x = 0; x < span; x++) {
                int current_grid_x = grid_x + x - (effectiveSize - 1);
                int current_grid_y = grid_y + y - (effectiveSize - 1);
                if (current_grid_x >= gridSize || current_grid_y >= gridSize || current_grid_x < 0 || current_grid_y < 0)
                    return false;
                if (grid.isOccupied(current_grid_x, current_grid_y)) return false;
            }
        }
        return true;
    }
}
