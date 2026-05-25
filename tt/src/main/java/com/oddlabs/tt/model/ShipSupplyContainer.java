package com.oddlabs.tt.model;

public final class ShipSupplyContainer extends SupplyContainer {
    private final ShipHR ship_hr;
    private final Class type;

    public ShipSupplyContainer(ShipHR ship_hr, Class type) {
        super(200);
        this.ship_hr = ship_hr;
        this.type = type;
    }

    public int getNumSupplies() {
        return ship_hr.countUnitsOfType(type);
    }
}
