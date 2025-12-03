package com.oddlabs.tt.pathfinder;


import org.jspecify.annotations.NonNull;

public final class TargetFinderAlgorithm<O extends Occupant> extends GridPathFinder {
	private final FinderFilter<O> filter;

	private O target;

	public TargetFinderAlgorithm(@NonNull UnitGrid unit_grid, FinderFilter<O> filter, Node dst_region, int dst_x, int dst_y, boolean allow_second_best) {
		super(unit_grid, dst_region, null, dst_x, dst_y, allow_second_best);
		this.filter = filter;
	}

	public O getOccupant() {
		return target;
	}

	@Override
	public boolean touchNeighbour(@NonNull Occupant occ) {
		if (filter.acceptOccupant(occ)) {
			target = (O) occ;
			return true;
		} else
			return false;
	}
}
