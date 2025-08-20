package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Building;
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
    private final boolean near_sea;
    private final int obj_radius;
    private final List result;

    public BuildingSiteScanFilter(
            UnitGrid unit_grid, BuildingTemplate template, int range, boolean one_target) {
        this.unit_grid = unit_grid;
        this.template = template;
        this.range = range;
        this.one_target = one_target;
        this.near_sea = template.isNearSea();
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
        boolean legal = Building.isPlacingLegal(unit_grid, template, grid_x, grid_y);
        if (!near_sea && unit_grid.getHeightMap().canBuild(grid_x, grid_y, obj_radius) && legal) {
            result.add(new LandscapeTarget(grid_x, grid_y));
            if (one_target) return true;
        }
        if (near_sea) {
            HeightMap map = unit_grid.getHeightMap();
            float sea = map.getSeaLevelMeters();
            int num_sea = 0;
            int x0 = grid_x + obj_radius;
            int x1 = grid_x - obj_radius;
            int y0 = grid_y + obj_radius;
            int y1 = grid_y - obj_radius;
            int grid_size = map.getAccessGrid().length;
            if (x0 < 0 || y0 < 0 || x1 >= grid_size || x1 >= grid_size) {
                return false;
            }
            for (int x = grid_x - obj_radius; x < grid_x + obj_radius; x++) {
                for (int y = grid_y - obj_radius; y < grid_y + obj_radius; y++) {
                    if (map.getClampedHeight(x, y) - sea < 1.0f) {
                        num_sea++;
                    }
                }
            }
            float percent = num_sea / (float) (obj_radius * obj_radius * 4);
            if (percent > 0.3f) {
                result.add(new LandscapeTarget(grid_x, grid_y));
                if (one_target) return true;
            }
        }
        return false;
    }

    public final List getResult() {
        return result;
    }
}
