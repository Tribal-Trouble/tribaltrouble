package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class TargetTrackerAlgorithm implements TrackerAlgorithm {
    private final UnitGrid unit_grid;
    private final Target target;
    private final float max_dist;

    public TargetTrackerAlgorithm(UnitGrid unit_grid, float max_dist, Target target) {
        this.unit_grid = unit_grid;
        this.max_dist = max_dist;
        this.target = target;
    }

    @Override
    public boolean isDone(int x, int y) {
        return target.isDead() || Selectable.isCloseEnough(unit_grid, max_dist, x, y, target);
    }

    @Override
    public boolean acceptRegion(@NonNull Region region) {
        return !target.isDead() && unit_grid.getRegion(target.getGridX(), target.getGridY()) == region;
    }

    @Override
    public @Nullable Region findPathRegion(int src_x, int src_y) {
        return target.isDead()
                ? null
                : PathFinder.findPathRegion(unit_grid,
                unit_grid.getRegion(src_x, src_y),
                unit_grid.getRegion(target.getGridX(), target.getGridY()));
    }

    @Override
    public @Nullable GridPathNode findPathGrid(@NonNull Region target_region, @NonNull Region next_region, int src_x, int src_y, boolean allow_secondary_targets) {
        return target.isDead()
                ? null
                : PathFinder.findPathGrid(unit_grid, next_region, null,
                src_x, src_y,
                target.getGridX(), target.getGridY(),
                target, max_dist, allow_secondary_targets);
    }
}
