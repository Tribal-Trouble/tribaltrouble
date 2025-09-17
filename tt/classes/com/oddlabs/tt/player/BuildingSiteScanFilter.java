package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.PlacementRules;
import com.oddlabs.tt.model.EditorGridReader;
import com.oddlabs.tt.model.RuntimeGridReader;
import com.oddlabs.tt.model.PlacementGridReader;
import com.oddlabs.tt.landscape.World;
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
    // Editor support: when world != null use editor grids (live) instead of runtime UnitGrid occupancy path
    private final World editorWorld;
    private final PlacementGridReader gridReader;

    public BuildingSiteScanFilter(
            UnitGrid unit_grid, BuildingTemplate template, int range, boolean one_target) {
        this.unit_grid = unit_grid;
        this.template = template;
        this.range = range;
        this.one_target = one_target;
        this.obj_radius = template.getPlacingSize() / 2;
        result = new ArrayList();
        this.editorWorld = null;
        this.gridReader = new RuntimeGridReader(unit_grid);
    }

    /** Editor variant using live editor grids (water/dock/build/access + editor occupancy). */
    public BuildingSiteScanFilter(World world, UnitGrid unit_grid, BuildingTemplate template, int range, boolean one_target) {
        this.unit_grid = unit_grid;
        this.template = template;
        this.range = range;
        this.one_target = one_target;
        this.obj_radius = template.getPlacingSize() / 2;
        result = new ArrayList();
        this.editorWorld = world;
        this.gridReader = new EditorGridReader(world);
    }

    public final int getMinRadius() {
        return 0;
    }

    public final int getMaxRadius() {
        return range;
    }

    public final boolean filter(int grid_x, int grid_y, Occupant occ) {
        boolean legal;
        if (editorWorld != null) {
            // Use shared rules directly to avoid stale static grids
            legal = PlacementRules.isPlacingLegal(
                gridReader,
                grid_x,
                grid_y,
                template.getPlacingSize(),
                template.isNearSea());
        } else {
            legal = Building.isPlacingLegal(unit_grid, template, grid_x, grid_y);
        }
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
