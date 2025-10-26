package com.oddlabs.tt.player;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class BuildingSiteScanFilter implements ScanFilter {
	private final UnitGrid unit_grid;
	private final BuildingTemplate template;
	private final int range;
	private final boolean one_target;
	private final List<LandscapeTarget> result = new ArrayList<>();

	public BuildingSiteScanFilter(UnitGrid unit_grid, BuildingTemplate template, int range, boolean one_target) {
		this.unit_grid = unit_grid;
		this.template = template;
		this.range = range;
		this.one_target = one_target;
	}

    @Override
	public int getMinRadius() {
		return 0;
	}

    @Override
	public int getMaxRadius() {
		return range;
	}

    @Override
	public boolean filter(int grid_x, int grid_y, Occupant occ) {
		if (unit_grid.getHeightMap().canBuild(grid_x, grid_y, template.getPlacingSize()) && Building.isPlacingLegal(unit_grid, template, grid_x, grid_y)) {
			result.add(new LandscapeTarget(grid_x, grid_y));
			if (one_target)
				return true;
		}
		return false;
	}

	public @NonNull List<LandscapeTarget> getResult() {
		return result;
	}
}
