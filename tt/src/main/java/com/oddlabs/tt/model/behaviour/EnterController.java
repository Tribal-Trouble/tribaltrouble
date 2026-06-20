package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.ThrowingFactory;
import org.jspecify.annotations.NonNull;

/**
 * Controller that handles unit movement and entry into buildings (towers or armories).
 */
public final class EnterController extends Controller {
    private final @NonNull Building building;
    private final @NonNull Unit unit;

    public EnterController(@NonNull Unit unit, @NonNull Building building) {
        super(1);
        this.unit = unit;
        this.building = building;
    }

    @Override
    public void decide() {
        if (building.isDead()) {
            unit.popController();
        } else if (unit.isCloseEnough(0f, building)) {
            building.getUnitContainer().ifPresentOrElse(c -> {
                if (c.canEnter(unit)) {
                    if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                        if (unit.getAbilities().hasAbilities(Abilities.HARVEST)
                                && unit.getSupplyContainer().getNumSupplies() > 0) {
                            unit.getSupplyContainer().getSupplyType().ifPresent(type -> building.getSupplyContainer(
                                    type)
                                    .orElseThrow().increaseSupply(unit.getSupplyContainer()
                                            .getNumSupplies())
                            );
                        }
                        if (unit.getWeaponFactory() instanceof ThrowingFactory) {
                            unit.getWeaponFactory().getType().ifPresent(type -> {
                                building.getSupplyContainer(type).orElseThrow().increaseSupply(1);
                            });
                        }
                    }
                    c.enter(unit);
                } else {
                    unit.popController();
                }
            }, unit::popController);
        } else {
            if (shouldGiveUp(0)) {
                unit.popController();
            } else
                unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
        }
    }
}
