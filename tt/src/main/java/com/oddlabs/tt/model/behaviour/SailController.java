package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final class SailController extends Controller {
    private final Ship ship;
    private final Target target;

    public SailController(Ship ship, Target t) {
        super(1);
        this.ship = ship;
        this.target = t;
    }

    public final void decide() {
        if (ship.isDead()) {
            return;
        }
        if (shouldGiveUp(0)) {
            ship.popController();
        } else {
            ship.setBehaviour(new SailBehaviour(ship, target, 0f));
        }
    }

    public final boolean isAgressive() {
        return false;
    }

    public final Target getTarget() {
        return target;
    }
}
