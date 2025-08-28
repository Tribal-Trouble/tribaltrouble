package com.oddlabs.tt.pathfinder;

public final strictfp class TargetRegionFinder implements PathFinderAlgorithm {
    private final FinderFilter filter;
    private final UnitGrid unit_grid;
    private final int layer;

    public TargetRegionFinder(UnitGrid unit_grid, FinderFilter filter, int layer) {
        this.unit_grid = unit_grid;
        this.filter = filter;
        this.layer = layer;
    }

    public final int getLayer() {
        return layer;
    }

    public final int computeEstimatedCost(Node node) {
        return 0;
    }

    public final boolean touchNeighbour(Occupant occ) {
        return false;
    }

    public final NodeResult touchNode(Node node) {
        Region region = (Region) node;
        Occupant occ = filter.getOccupantFromRegion(region, false);
        if (occ != null) {
            return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY(), layer));
        } else return null;
    }

    public final NodeResult getBestNode() {
        Occupant occ = filter.getBest();
        if (occ != null) {
            return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY(), layer));
        } else return null;
    }
}
