package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;

import java.util.*;

public final strictfp class BuildingSiteScanFilter implements ScanFilter {
    private final UnitGrid unit_grid;
    private final BuildingTemplate template;
    private final int range;
    private final boolean one_target;
    private final int obj_radius;
    private final List result;

    public BuildingSiteScanFilter(
            UnitGrid unit_grid, BuildingTemplate template, int range, boolean one_target) {
        this.unit_grid = unit_grid;
        this.template = template;
        this.range = range;
        this.one_target = one_target;
        this.obj_radius = template.getPlacingSize() / 2;
        result = new ArrayList();
    }

    public final int getMinRadius() {
        return 0;
    }

    public final int getMaxRadius() {
        return range;
    }

    public final boolean filter(int grid_x, int grid_y, Occupant occ) {
        boolean legal = template.isPlacingLegal(unit_grid, grid_x, grid_y);
        HeightMap map = unit_grid.getHeightMap();
        boolean can_build = map.canBuild(grid_x, grid_y, obj_radius) && !template.isNearSea();
        boolean can_dock = map.canDock(grid_x, grid_y) && template.isNearSea();
        if ((can_build || can_dock) && legal) {
            result.add(new LandscapeTarget(grid_x, grid_y));
            if (one_target) return true;
        }
        return false;
    }

    public final List getResult() {
        return result;
    }
}
