package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Controller that handles unit repair cycles, including gathering resources if needed.
 */
public final class RepairController extends Controller {
    private enum State {
        HARVEST,
        REPAIR
    }

    private final Building building;
    private final Unit unit;

    public RepairController(Unit unit, Building building) {
        super(State.values().length);
        this.unit = unit;
        this.building = building;
    }

    public Building getBuilding() {
        return building;
    }

    @Override
    public @NonNull String getKey() {
        return super.getKey() + building.hashCode();
    }

    @Override
    public void decide() {
        if (building.isDead()) {
            unit.popController();
        } else if (unit.getSupplyContainer().getSupplyType().orElse(null) == SupplyType.WOOD && unit
                .getSupplyContainer()
                .getNumSupplies() > 0) {
                    resetGiveUpCounter(State.HARVEST);
                    if (unit.isCloseEnough(0f, building)) {
                        if (building.isDamaged()) {
                            unit.setBehaviour(new RepairBehaviour(unit, building));
                        } else if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER) && unit.getOwner()
                                == building.getOwner()) {
                                    unit.swapController(new EnterController(unit, building));
                                } else {
                                    unit.popController();
                                }
                    } else {
                        if (shouldGiveUp(State.REPAIR)) {
                            unit.popController();
                        } else {
                            unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
                        }
                    }
                } else {
                    resetGiveUpCounter(State.REPAIR);
                    if (!shouldGiveUp(State.HARVEST)) {
                        unit.pushController(new HarvestController<>(unit, null, SupplyType.WOOD));
                    } else {
                        unit.popController();
                    }
                }
    }
}
