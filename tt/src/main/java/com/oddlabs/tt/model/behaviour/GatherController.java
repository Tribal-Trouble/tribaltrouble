package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingFinder;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Controller that coordinates peon gathering loop: moving to a resource, harvesting it, and dropping it off.
 */
public final class GatherController<S extends Supply> extends Controller {
    private enum State {
        HARVEST,
        DROPOFF
    }

    private final @NonNull Unit unit;
    private final @NonNull SupplyType supplyType;
    private @Nullable S supply;
    private @Nullable FinderTrackerAlgorithm<Building> building_tracker;

    public GatherController(@NonNull Unit unit, @Nullable S supply, @NonNull SupplyType supplyType) {
        super(State.values().length);
        this.unit = unit;
        this.supply = supply;
        this.supplyType = supplyType;
    }

    public @NonNull SupplyType getSupplyType() {
        return supplyType;
    }

    @Override
    public @NonNull String getKey() {
        return super.getKey() + supplyType;
    }

    private void gather() {
        resetGiveUpCounter(State.DROPOFF);
        if (supply != null && supply.isDead()) {
            supply = null;
            resetGiveUpCounter(State.HARVEST);
        }

        if (supply != null && unit.isCloseEnough(unit.getRange(supply), supply)) {
            unit.pushController(new HarvestController<>(unit, supply, supplyType));
        } else if (!shouldGiveUp(State.HARVEST)) {
            if (supply == null) {
                unit.pushController(new HarvestController<>(unit, supply, supplyType));
            } else {
                TargetTrackerAlgorithm supply_tracker = new TargetTrackerAlgorithm(unit.getUnitGrid(), 0f, supply);
                unit.setBehaviour(new WalkBehaviour(unit, supply_tracker, false));
            }
        } else {
            unit.swapController(new TransferUnitController(unit));
        }
    }

    private void dropoff() {
        resetGiveUpCounter(State.HARVEST);
        if (building_tracker != null && building_tracker.getOccupant() != null && unit.isCloseEnough(0f,
                building_tracker.getOccupant())) {
            Building building = building_tracker.getOccupant();
            unit.getSupplyContainer().getSupplyType().ifPresent(unit_supply_type -> {
                int num_supplies = building.getSupplyContainer(unit_supply_type).orElseThrow().increaseSupply(unit
                        .getSupplyContainer()
                        .getNumSupplies());
                unit.getSupplyContainer().increaseSupply(-num_supplies, unit_supply_type);
            });
            if (unit.getSupplyContainer().getNumSupplies() > 0) {
                unit.popController();
                unit.pushController(new EnterController(unit, building));
            } else
                gather();
        } else if (!shouldGiveUp(State.DROPOFF)) {
            building_tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(), new BuildingFinder(unit.getOwner(),
                    Abilities.SUPPLY_CONTAINER));
            unit.setBehaviour(new WalkBehaviour(unit, building_tracker, false));
        } else {
            unit.popController();
        }
    }

    @Override
    public void decide() {
        if (unit.getSupplyContainer().getNumSupplies() > 0 && unit.getSupplyContainer().getSupplyType().orElse(null)
                == supplyType) {
            dropoff();
        } else {
            gather();
        }
    }
}
