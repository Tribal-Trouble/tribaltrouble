package com.oddlabs.tt.pathfinder;

final class RegionPathFinder extends AStarAlgorithm {
	private final Node dst_region;

	public RegionPathFinder(UnitGrid unit_grid, Node dst_region) {
		super(unit_grid, dst_region.getGridX(), dst_region.getGridY(), false);
		this.dst_region = dst_region;
	}

    @Override
	public boolean touchNeighbour(Occupant occ) {
		return false;
	}

    @Override
	protected boolean isPathComplete(int dist_squared, Node node) {
		return node == dst_region;
	}
}
