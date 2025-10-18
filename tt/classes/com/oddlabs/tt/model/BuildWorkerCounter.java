package com.oddlabs.tt.model;

public final class BuildWorkerCounter<S extends Supply> extends SupplyCounter<S> {
	public BuildWorkerCounter(Building building, Class<S> supply_type) {
		super(building, supply_type);
		setDelta(building.getBuildSupplyContainer(supply_type).getNumOrders());
	}

    @Override
	public int getNumSupplies() {
		if (!getBuilding().isDead())
			return getBuilding().getSupplyContainer(getSupplyType()).getNumSupplies() - (getDelta() - getBuilding().getBuildSupplyContainer(getSupplyType()).getNumOrders());
		else
			return 0;
	}
}
