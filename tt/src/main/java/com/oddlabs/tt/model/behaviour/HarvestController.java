package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.SupplyFinder;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FinderTrackerAlgorithm;
import org.jspecify.annotations.Nullable;

public final class HarvestController<S extends Supply> extends Controller {
	private final Unit unit;
	private final Class<S> supply_class;
	private FinderTrackerAlgorithm<S> tracker;

	private @Nullable Supply supply;

	public HarvestController(Unit unit, S supply, Class<S> supply_class) {
		super(1);
		this.unit = unit;
		this.supply = supply;
		this.supply_class = supply_class;
	}

	private void gather() {
		if (supply != null && !supply.isEmpty() && unit.isCloseEnough(0f, supply)) {
			resetGiveUpCounter(0);
			unit.setBehaviour(new HarvestBehaviour(unit, supply));
		} else if (!shouldGiveUp(0)) {
			tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(), new SupplyFinder<>(unit, supply_class));
			unit.setBehaviour(new WalkBehaviour(unit, tracker, false));
		} else {
			unit.popController();
		}
	}

	@Override
	public void decide() {
		if (unit.getSupplyContainer().getSupplyType() == supply_class && unit.getSupplyContainer().isSupplyFull()) {
			unit.popController();
		} else {
			if (tracker != null) {
				supply = (Supply)tracker.getOccupant();
			}
			gather();
		}
	}
}
