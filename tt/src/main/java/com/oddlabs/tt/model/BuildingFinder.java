package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.FinderFilter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class BuildingFinder implements FinderFilter<Building> {
    private final @NonNull Player owner;
	private final int abilities;

	public BuildingFinder(@NonNull Player owner, int abilities) {
		this.owner = owner;
		this.abilities = abilities;
	}

	@Override
	public @Nullable Building getOccupantFromRegion(@NonNull Region region, boolean one_region) {
		return region.getObjects(Building.class).stream()
                .filter(this::accept)
                .findFirst()
                .orElse(null);
	}

	@Override
	public @Nullable Building getBest() {
		return null;
	}

	private boolean accept(@NonNull Building building) {
		return building.getOwner() == owner && building.getAbilities().hasAbilities(abilities);
	}

	@Override
	public boolean acceptOccupant(@NonNull Occupant occ) {
        return occ instanceof Building building && accept(building);
	}
}
