package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class SittingBehaviour implements Behaviour {
    private final SittingController controller;
    private final Unit unit;
    private final Ship ship;
    private final ShipAllocation allocation;
    private boolean boarded = false;
    private float boarding_time = 0.0f;
    private final static float TOTAL_BOARDING_TIME = 1.0f;

    public SittingBehaviour(
            SittingController controller, Unit unit, Ship ship, ShipAllocation allocation, boolean boarded) {
        this.controller = controller;
        this.unit = unit;
        this.ship = ship;
        this.allocation = allocation;
        this.boarded = boarded;
    }

    @Override
    public @NonNull State animate(float t) {
        if (unit.isDead()) {
            return State.DONE;
        }
        if (ship.isMoving() || boarding_time >= TOTAL_BOARDING_TIME) {
            boarded = true;
        }
        if (!boarded) {
            var proxy = ship.getEntrance();
            boarding_time += t;
            unit.switchAnimation(3.0f, Unit.Animation.MOVING);
            allocation.updateIntermediate(unit, ship, boarding_time / TOTAL_BOARDING_TIME);
        } else {
            switch (allocation.getRole()) {
                case ShipAllocation.SITTING:
                    unit.switchToSittingAnimation();
                    break;
                case ShipAllocation.STEERING:
                    unit.switchToSteeringAnimation();
                    break;
                case ShipAllocation.ROWING_RIGHT:
                    if (ship.isMoving()) {
                        unit.switchToRowingRightAnimation();
                    } else {
                        unit.switchToSittingAnimation();
                    }
                    break;
                case ShipAllocation.ROWING_LEFT:
                    if (ship.isMoving()) {
                        unit.switchToRowingLeftAnimation();
                    } else {
                        unit.switchToSittingAnimation();
                    }
                    break;
            }
            allocation.updateFinal(unit, ship);
        }
        return State.INTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return false;
    }

    public final void forceInterrupted() {
    }
}
