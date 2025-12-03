package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class WorkerUnitContainer extends UnitContainer {
	private final @NonNull Building building;

	public WorkerUnitContainer(@NonNull Building building) {
		super(building.getOwner().getWorld().getMaxUnitCount());
		this.building = building;
	}

	@Override
	public void enter(@NonNull Unit unit) {
		assert canEnter(unit);
		unit.removeNow();
		increaseSupply(1);
	}

	@Override
	public boolean canEnter(@NonNull Unit unit) {
		return getTotalSupplies() != getMaxSupplyCount();
	}

	private int getTotalSupplies() {
//		return getNumSupplies() + building.getBuildSupplyContainer(Unit.class).getNumSupplies() == getMaxSupplyCount();
		return getNumSupplies() + getNumPreparing();
	}

	@Override
	public @Nullable Unit exit() {
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
}
