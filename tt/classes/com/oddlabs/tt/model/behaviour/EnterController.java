package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitSupplyContainer;
import com.oddlabs.tt.model.weapon.ThrowingFactory;
import com.oddlabs.tt.pathfinder.UnitGrid;

public final strictfp class EnterController extends Controller {
    private final Building building;
    private final Unit unit;

    public EnterController(Unit unit, Building building) {
        super(1);
        this.unit = unit;
        this.building = building;
    }

    public final void decide() {
        if (building.isDead()) {
            unit.popController();
        } else if (unit.isCloseEnough(0f, building, UnitGrid.LAND)) {
            if (building.getUnitContainer() != null
                    && building.getUnitContainer().canEnter(unit)
                    && building.canAccommodate(unit)) {
                UnitSupplyContainer unitSupply = unit.getSupplyContainer();
                int numSupply = (unitSupply != null) ? unitSupply.getNumSupplies() : 0;
                if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                    if (unit.getAbilities().hasAbilities(Abilities.HARVEST) && numSupply > 0) {
                        Class type = unitSupply.getSupplyType();
                        building.getSupplyContainer(type).increaseSupply(numSupply);
                        unitSupply.increaseSupply(-numSupply, type);
                    }
                    if (unit.getWeaponFactory() instanceof ThrowingFactory) {
                        Class type = unit.getWeaponFactory().getType();
                        building.getSupplyContainer(type).increaseSupply(1);
                    }
                }
                if (building.canGetOnBoard()) {
                    building.getOnBoard(unit);
                    building.getOwner().getUnitCountContainer().increaseSupply(-1);
                    building.getUnitContainer().increaseSupply(1);
                } else {
                    building.getUnitContainer().enter(unit);
                }
            } else {
                unit.popController();
            }
        } else {
            if (shouldGiveUp(0)) {
                unit.popController();
            } else unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
        }
    }
}
