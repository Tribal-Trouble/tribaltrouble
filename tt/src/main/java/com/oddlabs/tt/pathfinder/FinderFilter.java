package com.oddlabs.tt.pathfinder;

import org.jspecify.annotations.Nullable;

public interface FinderFilter<O extends Occupant> {
	@Nullable O getOccupantFromRegion(Region region, boolean one_region);
	@Nullable O getBest();
	boolean acceptOccupant(Occupant occ);
}
