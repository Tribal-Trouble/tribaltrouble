package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class BuildWorkerCounter<S extends Supply> extends SupplyCounter {
    public BuildWorkerCounter(@NonNull Building building, @NonNull Class<S> supply_type) {
        super(building, supply_type);
        setDelta(building.getBuildSupplyContainer(supply_type).map(c -> c.getNumOrders()).orElse(0));
    }

    @Override
    public int getNumSupplies() {
        if (!getBuilding().isDead())
            return getBuilding().getSupplyContainer(getSupplyType()).map(c -> c.getNumSupplies()).orElse(0)
                    - (getDelta() - getBuilding()
                            .getBuildSupplyContainer(getSupplyType()).map(c -> c.getNumOrders()).orElse(0));
        else
            return 0;
    }
}
