package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingFinder;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;

public final strictfp class GatherController extends Controller {
    private static final int HARVEST_STATE = 0;
    private static final int DROPOFF_STATE = 1;

    private final Unit unit;
    private final Class supply_type;
    private Supply supply;
    private FinderTrackerAlgorithm building_tracker;

    public GatherController(Unit unit, Supply supply, Class supply_type) {
        super(2);
        this.unit = unit;
        this.supply = supply;
        this.supply_type = supply_type;
        // Register this unit as a harvester for this supply type
        unit.getOwner().getHarvesterTracker().registerHarvester(unit, supply_type);
    }

    public final Unit getUnit() {
        return unit;
    }

    public final Class getSupplyType() {
        return supply_type;
    }

    public final String getKey() {
        return super.getKey() + supply_type;
    }

    private final void gather() {
        resetGiveUpCounter(DROPOFF_STATE);
        if (supply != null && supply.isDead()) {
            supply = null;
            resetGiveUpCounter(HARVEST_STATE);
        }

        if (supply != null && unit.isCloseEnough(unit.getRange(supply), supply)) {
            unit.pushController(new HarvestController(unit, supply, supply_type));
        } else if (!shouldGiveUp(HARVEST_STATE)) {
            if (supply == null) {
                unit.pushController(new HarvestController(unit, supply, supply_type));
            } else {
                TargetTrackerAlgorithm supply_tracker =
                        new TargetTrackerAlgorithm(unit.getUnitGrid(), 0f, supply);
                unit.setBehaviour(new WalkBehaviour(unit, supply_tracker, false));
            }
        } else {
            // Unregister before stopping gathering
            unit.getOwner().getHarvesterTracker().unregisterHarvester(unit);
            unit.swapController(new TransferUnitController(unit));
        }
    }

    private final void dropoff() {
        resetGiveUpCounter(HARVEST_STATE);
        if (building_tracker != null
                && building_tracker.getOccupant() != null
                && unit.isCloseEnough(0f, building_tracker.getOccupant())) {
            Building building = (Building) building_tracker.getOccupant();
            Class unit_supply_type = unit.getSupplyContainer().getSupplyType();
            int num_supplies =
                    building.getSupplyContainer(unit_supply_type)
                            .increaseSupply(unit.getSupplyContainer().getNumSupplies());
            unit.getSupplyContainer().increaseSupply(-num_supplies, unit_supply_type);
            if (unit.getSupplyContainer().getNumSupplies() > 0) {
                // Resource container is full, unit enters building - stop gathering
                unit.getOwner().getHarvesterTracker().unregisterHarvester(unit);
                unit.popController();
                unit.pushController(new EnterController(unit, building));
            } else gather();
        } else if (!shouldGiveUp(DROPOFF_STATE)) {
            building_tracker =
                    new FinderTrackerAlgorithm(
                            unit.getUnitGrid(),
                            new BuildingFinder(unit.getOwner(), Abilities.SUPPLY_CONTAINER));
            unit.setBehaviour(new WalkBehaviour(unit, building_tracker, false));
        } else {
            // Can't find dropoff location, stop gathering
            unit.getOwner().getHarvesterTracker().unregisterHarvester(unit);
            unit.popController();
        }
    }

    public final void decide() {
        if (unit.getSupplyContainer().getNumSupplies() > 0
                && unit.getSupplyContainer().getSupplyType() == supply_type) {
            dropoff();
        } else {
            gather();
        }
    }
}
