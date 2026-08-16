package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.ShipTrajectory;
import com.oddlabs.tt.pathfinder.ShipTrajectoryPoint;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;


public final class ReverseSailBehaviour implements Behaviour {
    private final Ship ship;
    private boolean blocked = false;

    private final UnitGrid grid;

    public ReverseSailBehaviour(Ship ship) {
        this.ship = ship;
        grid = ship.getUnitGrid();
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append("ReverseSailBehaviour: ");
        if (blocked) {
            tool_tip_box.append("BLOCKED");
        } else {
            tool_tip_box.append("MOVING");
        }
    }

    @Override
    public @NonNull State animate(float t) {
        if (ship.isDead()) {
            return State.DONE;
        }

        if (t == 0.0f) {
            return State.UNINTERRUPTIBLE;
        }

        ship.setLayer(UnitGrid.SEA);

        ShipTrajectoryPoint shipPt = new ShipTrajectoryPoint(ship);

        int rowers = ship.getShipHR().countRowers();
        if (rowers == 0) {
            ship.endTrip();
            return State.DONE;
        }

        // If it's clear infront of the ship
        if (!ShipTrajectory.checkLandCollision(grid, shipPt, shipPt.moved(20))
                && !ShipTrajectory.checkShipsCollision(grid, ship, shipPt, shipPt.moved(20))) {
            ship.reportStuck();
            return State.INTERRUPTIBLE;
        }

        float step = rowers * 0.2f * t;
        var next = shipPt.moved(-step);

        if (ShipTrajectory.checkLandCollision(grid, shipPt, next)) {
            ship.reportStuck();
            return State.INTERRUPTIBLE;
        } else if (ShipTrajectory.checkShipsCollision(grid, ship, shipPt, next.moved(-8))) {
            return State.UNINTERRUPTIBLE;
        }

        ship.free();
        ship.setPosition(next.positionX, next.positionY);
        ship.setGridPosition(next.gridX, next.gridY);
        ship.occupy();

        return State.UNINTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return blocked;
    }

    public final void forceInterrupted() {
    }
}
