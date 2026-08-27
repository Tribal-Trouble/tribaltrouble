package com.oddlabs.tt.model;

public final class ShipUnitContainer extends UnitContainer {
    private final Ship ship;

    public ShipUnitContainer(Ship ship) {
        super(ship.getOwner().getWorld().getMaxUnitCount());
        this.ship = ship;
    }

    public final void enter(Unit unit) {
        ShipAllocation allocation = ship.getShipHR().tryAllocate(unit);
        unit.mount(ship, allocation);
    }

    public final boolean canEnter(Unit unit) {
        return ship.getShipHR().canAllocate(unit);
    }

    private final int getTotalSupplies() {
        return getNumSupplies() + getNumPreparing();
    }

    public int getNumSupplies() {
        return ship.getShipHR().countUnits();
    }

    public int capAmount(int amount) {
        int supply_count = getNumSupplies();
        return Math.max(supply_count + amount, 0) - supply_count;
    }

    public final Unit exit() {
        return null;
    }

    public int increaseSupply(int amount) {
        return super.increaseSupply(amount);
    }

    public final void animate(float t) {
    }
}
