package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.SupplyFinder;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Controller that coordinates finding resource targets and dispatching peons to harvest them.
 */
public final class HarvestController<S extends Supply> extends Controller {
    private final @NonNull Unit unit;
    private final @NonNull SupplyType supplyType;
    private FinderTrackerAlgorithm<S> tracker;

    private @Nullable Supply supply;

    public HarvestController(@NonNull Unit unit, @Nullable S supply, @NonNull SupplyType supplyType) {
        super(1);
        this.unit = unit;
        this.supply = supply;
        this.supplyType = supplyType;
    }

    private void gather() {
        if (supply != null && !supply.isEmpty() && unit.isCloseEnough(0f, supply)) {
            resetGiveUpCounter(0);
            unit.setBehaviour(new HarvestBehaviour(unit, supply));
        } else if (!shouldGiveUp(0)) {
            tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(), new SupplyFinder<>(unit, supplyType));
            unit.setBehaviour(new WalkBehaviour(unit, tracker, false));
        } else {
            unit.popController();
        }
    }

    @Override
    public void decide() {
        if (unit.getSupplyContainer().getSupplyType().orElse(null) == supplyType && unit.getSupplyContainer()
                .isSupplyFull()) {
            unit.popController();
        } else {
            if (tracker != null) {
                supply = tracker.getOccupant();
            }
            gather();
        }
    }
}
