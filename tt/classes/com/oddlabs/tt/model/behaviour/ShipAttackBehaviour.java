package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;

public final strictfp class ShipAttackBehaviour implements Behaviour {
    private final ShipAttackController controller;
    private final Unit unit;
    private final Building boat;
    private final ShipAllocation allocation;

    public ShipAttackBehaviour(
            ShipAttackController controller, Unit unit, Building boat, ShipAllocation allocation) {
        this.controller = controller;
        this.unit = unit;
        this.boat = boat;
        this.allocation = allocation;
    }

    public final int animate(float t) {
        if (unit.isDead()) {
            return Selectable.DONE;
        }
        unit.switchToIdleAnimation();
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
        float rx = allocation.getRotation().x;
        float ry = allocation.getRotation().y;
        unit.setDirection(rx * dx - ry * dy, ry * dx + rx * dy);
        if (!controller.shouldSleep(t)) return Selectable.DONE;
        else return Selectable.INTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return true;
    }

    public final void forceInterrupted() {}
}
