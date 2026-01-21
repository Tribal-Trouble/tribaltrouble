package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;

public final strictfp class AttackController extends Controller {

    private final Selectable target;
    private final Unit unit;
    private final Building boat;
    private final ShipAllocation allocation;

    public AttackController(Unit unit, Selectable target) {
        super(0);
        this.unit = unit;
        this.target = target;
        this.boat = null;
        this.allocation = null;
    }

    public AttackController(
            Unit unit, Selectable target, ShipAllocation allocation, Building boat) {
        super(0);
        this.unit = unit;
        this.target = target;
        this.boat = boat;
        this.allocation = allocation;
    }

    private final boolean canAttack() {
        return unit.isCloseEnough(unit.getRange(target), target, target.getLayer());
    }

    public final void decide() {
        if (target.isDead() || !canAttack()) {
            unit.popController();
        } else {
            unit.setBehaviour(new AttackBehaviour(unit, target, allocation, boat));
        }
    }
}
