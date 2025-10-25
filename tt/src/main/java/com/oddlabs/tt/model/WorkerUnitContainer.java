package com.oddlabs.tt.model;

public final class WorkerUnitContainer extends UnitContainer {
	private final Building building;

	public WorkerUnitContainer(Building building) {
		super(building.getOwner().getWorld().getMaxUnitCount());
		this.building = building;
	}

        @Override
	public void enter(Unit unit) {
		assert canEnter(unit);
		unit.removeNow();
		increaseSupply(1);
	}

        @Override
	public boolean canEnter(Unit unit) {
		return getTotalSupplies() != getMaxSupplyCount();
	}

	private int getTotalSupplies() {
//		return getNumSupplies() + building.getBuildSupplyContainer(Unit.class).getNumSupplies() == getMaxSupplyCount();
		return getNumSupplies() + getNumPreparing();
	}

        @Override
	public Unit exit() {
		assert getNumSupplies() > 0;
		increaseSupply(-1);
		return null;
	}

        @Override
	public int increaseSupply(int amount) {
		int result = building.getOwner().getUnitCountContainer().increaseSupply(amount);
		assert result == amount: "result = " + result + " | amount = " + amount;
		return super.increaseSupply(amount);
	}

        @Override
	public void animate(float t) {
	}
}
