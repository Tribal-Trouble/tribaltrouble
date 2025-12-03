package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingFinder;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
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
	private @Nullable FinderTrackerAlgorithm<Building> building_tracker;

	public GatherController(@NonNull Unit unit, @Nullable S supply, @NonNull Class<S> supply_type) {
		super(State.values().length);
		this.unit = unit;
		this.supply = supply;
		this.supply_type = supply_type;
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

	private void dropoff() {
		resetGiveUpCounter(State.HARVEST.ordinal());
		if (building_tracker != null && building_tracker.getOccupant() != null && unit.isCloseEnough(0f, building_tracker.getOccupant())) {
			Building building = building_tracker.getOccupant();
			Class<? extends Supply> unit_supply_type = unit.getSupplyContainer().getSupplyType();
			int num_supplies = building.getSupplyContainer(unit_supply_type).increaseSupply(unit.getSupplyContainer().getNumSupplies());
			unit.getSupplyContainer().increaseSupply(-num_supplies, unit_supply_type);
			if (unit.getSupplyContainer().getNumSupplies() > 0) {
				unit.popController();
				unit.pushController(new EnterController(unit, building));
			} else
				gather();
		} else if (!shouldGiveUp(State.DROPOFF.ordinal())) {
			building_tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(), new BuildingFinder(unit.getOwner(), Abilities.SUPPLY_CONTAINER));
			unit.setBehaviour(new WalkBehaviour(unit, building_tracker, false));
		} else {
			unit.popController();
		}
	}

	@Override
	public void decide() {
		if (unit.getSupplyContainer().getNumSupplies() > 0 && unit.getSupplyContainer().getSupplyType() == supply_type) {
			dropoff();
		} else {
			gather();
		}
	}
}
