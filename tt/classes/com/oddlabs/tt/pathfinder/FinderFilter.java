package com.oddlabs.tt.pathfinder;

public interface FinderFilter<O extends Occupant> {
	O getOccupantFromRegion(Region region, boolean one_region);
	O getBest();
	boolean acceptOccupant(Occupant occ);
}
