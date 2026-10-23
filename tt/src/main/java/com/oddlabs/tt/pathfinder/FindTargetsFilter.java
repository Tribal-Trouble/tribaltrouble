package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class FindTargetsFilter implements ScanFilter {

    private final UnitGrid grid;
    private final int max_radius;
    private final @Nullable Target @NonNull [] result;
    private final boolean checkerboard_positions_only;
    private int index;
    private int island;

    public FindTargetsFilter(UnitGrid grid, int num_targets, int max_radius, boolean checkerboard_positions_only,
            int island) {
        this.grid = grid;
        result = new @Nullable Target[num_targets];
        this.max_radius = max_radius;
        this.checkerboard_positions_only = checkerboard_positions_only;
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
        if ((!checkerboard_positions_only || ((grid_x + grid_y) & 1) == 0) && occupant == null && (island == -1
                || grid.getIslandId(grid_x, grid_y) == island)) {
            result[index] = new LandscapeTarget(grid_x, grid_y);
            index++;
        }
        return index == result.length;
    }

    public @Nullable Target @NonNull [] getTargets() {
        return result;
    }
}
