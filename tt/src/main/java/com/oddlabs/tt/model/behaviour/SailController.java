package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final class SailController extends Controller {
    private final Ship ship;
    private final Target target;
    private boolean was_sliding = false;

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
            if (was_sliding) {
                ship.setBehaviour(new SailBehaviour(ship, target));
                was_sliding = false;
                resetGiveUpCounter(0);
            } else {
                ship.popController();
            }
        } else {
            if (!ship.slid()) {
                was_sliding = true;
                ship.setBehaviour(new ShipSlideBehaviour(ship));
            } else {
                was_sliding = false;
                ship.setBehaviour(new SailBehaviour(ship, target));
            }
        }
    }
}
