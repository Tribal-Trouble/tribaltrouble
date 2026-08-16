package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;

import java.util.ArrayList;
import java.util.List;

public final class ShipTargetScanFilter implements ScanFilter {
    private final UnitGrid unit_grid;
    private final int range;
    private final List result;
    private final boolean water_target;

    public ShipTargetScanFilter(UnitGrid unit_grid, Ship ship, int range, boolean water) {
        this.unit_grid = unit_grid;
        this.range = range;
        this.result = new ArrayList();
        this.water_target = water;
    }

    public final int getMinRadius() {
        return 0;
    }

    public final int getMaxRadius() {
        return range;
    }

    public final boolean filter(int grid_x, int grid_y, Occupant occ) {
        if (!water_target) {
            if (unit_grid.isDockable(grid_x, grid_y)
                    && unit_grid.getRegion(grid_x, grid_y, UnitGrid.LAND) != null) {
                result.add(new LandscapeTarget(grid_x, grid_y));
            }
        } else {
            if (unit_grid.isWater(grid_x, grid_y) && !unit_grid.isDockable(grid_x, grid_y)) {
                result.add(new LandscapeTarget(grid_x, grid_y));
            }
        }
        return false;
    }

    public final List getResult() {
        return result;
    }
}
