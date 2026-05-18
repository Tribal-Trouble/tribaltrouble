package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

final class TargetGridPathFinder extends GridPathFinder {
    private final float max_dist_squared;
    private final Target target;

    public TargetGridPathFinder(@NonNull UnitGrid unit_grid, float max_dist, Node dst_region, Node dst_region2,
            int dst_x, int dst_y, Target t, boolean allow_second_best) {
        super(unit_grid, dst_region, dst_region2, dst_x, dst_y, allow_second_best);
        this.max_dist_squared = max_dist * max_dist;
        this.target = t;
    }

    @Override
    public boolean touchNeighbour(Occupant occ) {
        return occ == target;
    }

    @Override
    protected boolean isPathComplete(int dist_squared, Node node) {
        return dist_squared <= max_dist_squared || super.isPathComplete(dist_squared, node);
    }
}
