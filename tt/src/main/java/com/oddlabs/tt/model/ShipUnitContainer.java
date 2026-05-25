package com.oddlabs.tt.model;

public final class ShipUnitContainer extends UnitContainer {
    private final Ship building;

    public ShipUnitContainer(Ship building) {
        super(building.getOwner().getWorld().getMaxUnitCount());
        this.building = building;
    }

    public final void enter(Unit unit) {
        ShipAllocation allocation = building.getShipHR().tryAllocate(unit);
        unit.mountDeck(building, allocation);
        increaseSupply(1);
    }

    public final boolean canEnter(Unit unit) {
        return building.getShipHR().canAllocate(unit);
    }

    private final int getTotalSupplies() {
        return getNumSupplies() + getNumPreparing();
    }

    public int getNumSupplies() {
        return building.getShipHR().countUnits();
    }

    public final Unit exit() {
        assert getNumSupplies() > 0;
        increaseSupply(-1);
        return null;
    }

    public int increaseSupply(int amount) {
        return super.increaseSupply(amount);
    }

    public final void animate(float t) {
    }
}
