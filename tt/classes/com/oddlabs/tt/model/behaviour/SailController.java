package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final strictfp class SailController extends Controller {
    private final Ship boat;
    private final Target target;

    public SailController(Ship boat, Target t) {
        super(1);
        this.boat = boat;
        this.target = t;
    }

    public final void decide() {
        if (shouldGiveUp(0)) {
            boat.popController();
        } else {
            boat.setBehaviour(new SailBehaviour(boat, target, 0f));
        }
    }

    public final boolean isAgressive() {
        return false;
    }

    public final Target getTarget() {
        return target;
    }
}
