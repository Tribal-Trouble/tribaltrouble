package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingFinder;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TrackerAlgorithm;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class GatherController<S extends Supply> extends Controller {
    private enum State {
        HARVEST,
        DROPOFF
    }

    private final @NonNull Unit unit;
    private final @NonNull Class<S> supply_type;
    private @Nullable S supply;
    private @Nullable FinderTrackerAlgorithm<Building> unknown_building_tracker;
    private @Nullable TargetTrackerAlgorithm assigned_building_tracker;
    private @Nullable Building assigned_building;

    public GatherController(@NonNull Unit unit, @Nullable S supply, @NonNull Class<S> supply_type) {
        super(State.values().length);
        this.unit = unit;
        this.supply = supply;
        this.supply_type = supply_type;
        this.assigned_building = null;
    }

    public GatherController(@NonNull Unit unit, @Nullable S supply, @NonNull Class<S> supply_type, Building building) {
        super(State.values().length);
        this.unit = unit;
        this.supply = supply;
        this.supply_type = supply_type;
        this.assigned_building = building;
    }

    public @NonNull Class<S> getSupplyType() {
        return supply_type;
    }

    @Override
    public @NonNull String getKey() {
        return super.getKey() + supply_type;
    }

    private void gather() {
        resetGiveUpCounter(State.DROPOFF.ordinal());
        if (supply != null && supply.isDead()) {
            supply = null;
            resetGiveUpCounter(State.HARVEST.ordinal());
        }

        if (supply != null && unit.isCloseEnough(unit.getRange(supply), supply)) {
            unit.pushController(new HarvestController<>(unit, supply, supply_type));
        } else if (!shouldGiveUp(State.HARVEST.ordinal())) {
            if (supply == null) {
                unit.pushController(new HarvestController<>(unit, supply, supply_type));
            } else {
                TargetTrackerAlgorithm supply_tracker = new TargetTrackerAlgorithm(unit.getUnitGrid(), 0f, supply);
                unit.setBehaviour(new WalkBehaviour(unit, supply_tracker, false));
            }
        } else {
            unit.swapController(new TransferUnitController(unit));
        }
    }

    private @Nullable Building getBuilding() {
        if (assigned_building != null) {
            return assigned_building.getEntrance();
        } else if (unknown_building_tracker != null) {
            Building building = unknown_building_tracker.getOccupant();
            if (building != null) {
                building = building.getEntrance();
            }
            return building;
        }
        return null;
    }

    private void dropoff() {
        resetGiveUpCounter(State.HARVEST.ordinal());
        Building building = getBuilding();
        if (building != null && unit.isCloseEnough(0f, building)) {
            Class<? extends Supply> unit_supply_type = unit.getSupplyContainer().getSupplyType();
            int num_supplies = building.getSupplyContainer(unit_supply_type).increaseSupply(
                    unit.getSupplyContainer().getNumSupplies());
            unit.getSupplyContainer().increaseSupply(-num_supplies, unit_supply_type);
            if (unit.getSupplyContainer().getNumSupplies() > 0) {
                unit.popController();
                unit.pushController(new EnterController(unit, building));
            } else
                gather();
        } else if (!shouldGiveUp(State.DROPOFF.ordinal())) {
            if (assigned_building == null || assigned_building.isDead()) {
                unknown_building_tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(), new BuildingFinder(
                        unit.getOwner(), Abilities.SUPPLY_CONTAINER));
                assigned_building_tracker = null;
            } else {
                assigned_building_tracker = new TargetTrackerAlgorithm(
                        unit.getUnitGrid(),
                        0.0f,
                        assigned_building.getEntrance());
                unknown_building_tracker = null;
            }

            unit.setBehaviour(new WalkBehaviour(unit, getTracker(), false));
        } else {
            unit.popController();
        }
    }

    private TrackerAlgorithm getTracker() {
        return assigned_building_tracker != null ? assigned_building_tracker : unknown_building_tracker;
    }

    @Override
    public void decide() {
        if (unit.getSupplyContainer().getNumSupplies() > 0
                && unit.getSupplyContainer().getSupplyType() == supply_type) {
            dropoff();
        } else {
            gather();
        }
    }
}
