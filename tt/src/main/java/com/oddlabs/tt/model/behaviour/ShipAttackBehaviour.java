package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.ShipAllocation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;

public final class ShipAttackBehaviour implements Behaviour {
    private final ShipAttackController controller;
    private final Unit unit;
    private final Ship ship;
    private final ShipAllocation allocation;

    public ShipAttackBehaviour(
            ShipAttackController controller, Unit unit, Ship ship, ShipAllocation allocation) {
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
        unit.switchToIdleAnimation();
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
        float rx = allocation.getRotation().x;
        float ry = allocation.getRotation().y;
        unit.setDirection(rx * dx - ry * dy, ry * dx + rx * dy);
        if (!controller.shouldSleep(t)) return State.DONE;
        else return State.INTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return true;
    }

    public final void forceInterrupted() {}
}
