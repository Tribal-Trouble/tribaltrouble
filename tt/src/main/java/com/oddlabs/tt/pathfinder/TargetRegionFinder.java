package com.oddlabs.tt.pathfinder;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class TargetRegionFinder implements PathFinderAlgorithm {
    private final @NonNull FinderFilter<?> filter;
    private final @NonNull UnitGrid unit_grid;

    public TargetRegionFinder(@NonNull UnitGrid unit_grid, @NonNull FinderFilter<?> filter) {
        this.unit_grid = unit_grid;
        this.filter = filter;
    }

    @Override
    public int computeEstimatedCost(Node node) {
        return 0;
    }

    @Override
    public boolean touchNeighbour(Occupant occ) {
        return false;
    }

    @Override
    public @Nullable NodeResult touchNode(Node node) {
        Region region = (Region) node;
        Occupant occ = filter.getOccupantFromRegion(region, false);
        if (occ != null) {
            return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY()));
        } else
            return null;
    }

    @Override
    public @Nullable NodeResult getBestNode() {
        Occupant occ = filter.getBest();
        if (occ != null) {
            return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY()));
        } else
            return null;
    }
}
