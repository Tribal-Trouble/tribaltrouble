package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FindTargetsFilter implements ScanFilter {

    private final UnitGrid grid;
    private final int max_radius;
    private final Target @NonNull [] result;
    private final boolean grid_targets_only;
    private int index;
    private int island;

    public FindTargetsFilter(UnitGrid grid, int num_targets, int max_radius, boolean grid_targets_only, int island) {
        this.grid = grid;
        result = new Target[num_targets];
        this.max_radius = max_radius;
        this.grid_targets_only = grid_targets_only;
        index = 0;
        this.island = island;
    }

    @Override
    public int getMinRadius() {
        return 0;
    }

    @Override
    public int getMaxRadius() {
        return max_radius;
    }

    @Override
    public boolean filter(int grid_x, int grid_y, @Nullable Occupant occupant) {
        if ((!grid_targets_only || ((grid_x + grid_y) & 1) == 0) && occupant == null && (island == -1
                || grid.getIslandId(grid_x, grid_y) == island)) {
            result[index] = new LandscapeTarget(grid_x, grid_y);
            index++;
        }
        return index == result.length;
    }

    public Target @NonNull [] getTargets() {
        return result;
    }
}
