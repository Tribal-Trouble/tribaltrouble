package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.FinderFilter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class BuildingFinder implements FinderFilter<Building> {
	private final Player owner;
	private final int abilities;

	public BuildingFinder(Player owner, int abilities) {
		this.owner = owner;
		this.abilities = abilities;
	}

    @Override
	public @Nullable Building getOccupantFromRegion(@NonNull Region region, boolean one_region) {
		List<Building> buildings = region.getObjects(Building.class);
		for (Building b : buildings) {
			if (accept(b))
				return b;
		}
		return null;
	}

    @Override
	public @Nullable Building getBest() {
		return null;
	}

	private boolean accept(@NonNull Building building) {
		return building.getOwner() == owner && building.getAbilities().hasAbilities(abilities);
	}

    @Override
	public boolean acceptOccupant(Occupant occ) {
		if (occ instanceof Building building) {
			return accept(building);
		} else
			return false;
	}
}
