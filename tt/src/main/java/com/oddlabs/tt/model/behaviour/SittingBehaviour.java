package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;

public final class SittingBehaviour implements Behaviour {
    private final SittingController controller;
    private final Unit unit;
    private final Ship ship;
    private final ShipAllocation allocation;

    public SittingBehaviour(
            SittingController controller, Unit unit, Ship ship, ShipAllocation allocation) {
        this.controller = controller;
        this.unit = unit;
        this.ship = ship;
        this.allocation = allocation;
    }

    @Override
    public @NonNull State animate(float t) {
        if (unit.isDead()) {
            return State.DONE;
        }
        switch (allocation.getRole()) {
            case ShipAllocation.SITTING:
                unit.switchToSittingAnimation();
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
        float x = ship.getPositionX();
        float y = ship.getPositionY();
        float dx = ship.getDirectionX();
        float dy = ship.getDirectionY();
        float ox = allocation.getOffset().x;
        float oy = allocation.getOffset().y;
        float gx = x + dx * ox - dy * oy;
        float gy = y + dy * ox + dx * oy;
        unit.setPosition(gx, gy);
        unit.setGridPosition(UnitGrid.toGridCoordinate(gx), UnitGrid.toGridCoordinate(gy));
        unit.setDirection(-dy, dx);
        return State.INTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return false;
    }

    public final void forceInterrupted() {
    }
}
