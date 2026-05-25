package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;

public final class AttackController extends Controller {

    private final Selectable<?> target;
    private final Unit unit;
    private final Ship ship;
    private final ShipAllocation allocation;

    public AttackController(Unit unit, Selectable<?> target) {
        super(0);
        this.unit = unit;
        this.target = target;
        this.ship = null;
        this.allocation = null;
    }

    public AttackController(Unit unit, Selectable target, ShipAllocation allocation, Ship ship) {
        super(0);
        this.unit = unit;
        this.target = target;
        this.ship = ship;
        this.allocation = allocation;
    }

    private boolean canAttack() {
        return unit.isCloseEnough(unit.getRange(target), target, target.getLayer());
    }

    @Override
    public void decide() {
        if (target.isDead() || !canAttack()) {
            unit.popController();
        } else {
            unit.setBehaviour(new AttackBehaviour(unit, target, allocation, ship));
        }
    }
}
