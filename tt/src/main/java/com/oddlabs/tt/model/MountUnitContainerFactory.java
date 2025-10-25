package com.oddlabs.tt.model;

public final class MountUnitContainerFactory implements UnitContainerFactory {
        @Override
	public UnitContainer createContainer(Building building) {
		return new MountUnitContainer(building);
	}
}
