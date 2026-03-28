package com.oddlabs.tt.model;

public final strictfp class ShipUnitContainerFactory implements UnitContainerFactory {
    public final UnitContainer createContainer(Building building) {
        return new ShipUnitContainer(building);
    }
}
