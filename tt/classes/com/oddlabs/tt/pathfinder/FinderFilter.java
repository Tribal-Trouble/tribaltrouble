package com.oddlabs.tt.pathfinder;

public interface FinderFilter<O extends Occupant> {
	public O getOccupantFromRegion(Region region, boolean one_region);
	public O getBest();
	public boolean acceptOccupant(Occupant occ);
}
