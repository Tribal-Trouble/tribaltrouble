package com.oddlabs.tt.model;

public sealed interface UnitContainerFactory permits MountUnitContainerFactory, ReproduceUnitContainerFactory,
        WorkerUnitContainerFactory {
    UnitContainer createContainer(Building building);
}
