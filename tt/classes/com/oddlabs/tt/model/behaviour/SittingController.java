package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;

public final strictfp class SittingController extends Controller {

    private final Unit unit;
    private final SittingBehaviour sitting_behaviour;

    public SittingController(Unit unit, Ship boat, ShipAllocation allocation) {
        super(0);
        this.unit = unit;
        this.sitting_behaviour = new SittingBehaviour(this, unit, boat, allocation);
    }

    public final boolean shouldSleep(float t) {
        return false;
    }

    public final void decide() {
        unit.setBehaviour(sitting_behaviour);
    }
}
