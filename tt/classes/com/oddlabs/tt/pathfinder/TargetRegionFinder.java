package com.oddlabs.tt.pathfinder;

public final class TargetRegionFinder implements PathFinderAlgorithm {
	private final FinderFilter filter;
	private final UnitGrid unit_grid;

	public TargetRegionFinder(UnitGrid unit_grid, FinderFilter filter) {
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
	public NodeResult touchNode(Node node) {
		Region region = (Region)node;
		Occupant occ = filter.getOccupantFromRegion(region, false);
		if (occ != null) {
			return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY()));
		} else
			return null;
	}

    @Override
	public NodeResult getBestNode() {
		Occupant occ = filter.getBest();
		if (occ != null) {
			return new NodeResult(unit_grid.getRegion(occ.getGridX(), occ.getGridY()));
		} else
			return null;
	}
}
