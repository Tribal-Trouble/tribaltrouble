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

    public int capAmount(int amount) {
        int supplies = getNumSupplies();
        return Math.clamp(supplies + amount, 0, max_supply_count) - supplies;
    }
}
