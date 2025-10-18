package com.oddlabs.tt.model;

public final class WorkerUnitContainerFactory implements UnitContainerFactory {
        @Override
	public UnitContainer createContainer(Building building) {
		return new WorkerUnitContainer(building);
	}
}
