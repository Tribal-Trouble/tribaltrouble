package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final class SailController extends Controller {
    private static final int MAX_FAILED_UNSTUCK_ATTEMPTS = 3;

    private final Ship ship;
    private final Target target;
    private Behaviour current = null;
    private int failed_unstuck_attempts = 0;

    public SailController(Ship ship, Target t) {
        super(0);
        this.ship = ship;
        this.target = t;
    }

    public final void decide() {
        if (ship.isDead()) {
            return;
        }
        switch (current) {
            case null -> current = ship.slid() ? new SailBehaviour(ship, target) : new ShipSlideBehaviour(ship);
            case ShipSlideBehaviour slide -> {
                if (ship.slid()) {
                    current = new SailBehaviour(ship, target);
                }
            }
            case SailBehaviour sail -> {
                if (sail.isStuck()) {
                    current = new ShipUnstuckBehaviour(ship);
                }
            }
            case ShipUnstuckBehaviour unstuck -> {
                if (unstuck.isFinished()) {
                    failed_unstuck_attempts = 0;
                    current = new SailBehaviour(ship, target);
                } else if (unstuck.hasFailed()) {
                    failed_unstuck_attempts++;
                    if (failed_unstuck_attempts >= MAX_FAILED_UNSTUCK_ATTEMPTS) {
                        ship.popController();
                        return;
                    }
                    current = new SailBehaviour(ship, target);
                }
            }
            default -> {
            }
        }
        ship.setBehaviour(current);
    }
}
