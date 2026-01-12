package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;

public final strictfp class SittingBehaviour implements Behaviour {
    private final SittingController controller;
    private final Unit unit;
    private final Building boat;
    private final ShipAllocation allocation;

    public SittingBehaviour(
            SittingController controller, Unit unit, Building boat, ShipAllocation allocation) {
        this.controller = controller;
        this.unit = unit;
        this.boat = boat;
        this.allocation = allocation;
    }

    public final int animate(float t) {
        switch (allocation.getRole()) {
            case ShipAllocation.SITTING:
                unit.switchToSittingAnimation();
                break;
            case ShipAllocation.ROWING_RIGHT:
                if (boat.isMoving()) {
                    unit.switchToRowingRightAnimation();
                } else {
                    unit.switchToSittingAnimation();
                }
                break;
            case ShipAllocation.ROWING_LEFT:
                if (boat.isMoving()) {
                    unit.switchToRowingLeftAnimation();
                } else {
                    unit.switchToSittingAnimation();
                }
                break;
        }
        float x = boat.getPositionX();
        float y = boat.getPositionY();
        float dx = boat.getDirectionX();
        float dy = boat.getDirectionY();
        float ox = allocation.getOffset().x;
        float oy = allocation.getOffset().y;
        float gx = x + dx * ox - dy * oy;
        float gy = y + dy * ox + dx * oy;
        unit.setPosition(gx, gy);
        unit.setGridPosition(UnitGrid.toGridCoordinate(gx), UnitGrid.toGridCoordinate(gy));
        unit.setDirection(-dy, dx);
        return Selectable.INTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return false;
    }

    public final void forceInterrupted() {}
}
