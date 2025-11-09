package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class MountUnitContainerFactory implements UnitContainerFactory {
	@Override
	public @NonNull UnitContainer createContainer(Building building) {
		return new MountUnitContainer(building);
	}
}
