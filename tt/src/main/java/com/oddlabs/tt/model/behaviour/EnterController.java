package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.ThrowingFactory;
import com.oddlabs.tt.model.weapon.ThrowingWeapon;
import org.jspecify.annotations.NonNull;

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
            if (building.getUnitContainer() != null && building.getUnitContainer().canEnter(unit)) {
                if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                    if (unit.getAbilities().hasAbilities(Abilities.HARVEST)
                            && unit.getSupplyContainer().getNumSupplies() > 0) {
                        Class<? extends Supply> type = unit.getSupplyContainer().getSupplyType();
                        building.getSupplyContainer(type).increaseSupply(unit.getSupplyContainer().getNumSupplies());
                    }
                    if (unit.getWeaponFactory() instanceof ThrowingFactory) {
                        Class<? extends ThrowingWeapon> type = unit.getWeaponFactory().getType();
                        building.getSupplyContainer(type).increaseSupply(1);
                    }
                }
                building.getUnitContainer().enter(unit);
            } else {
                unit.popController();
            }
        } else {
            if (shouldGiveUp(0)) {
                unit.popController();
            } else
                unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
        }
    }
}
