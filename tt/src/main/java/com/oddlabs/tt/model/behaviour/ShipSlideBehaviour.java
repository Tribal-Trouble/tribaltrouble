package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.ShipTrajectory;
import com.oddlabs.tt.pathfinder.ShipTrajectoryPoint;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;


public final class ShipSlideBehaviour implements Behaviour {
    private final Ship ship;
    private boolean blocked = false;
    private ShipTrajectoryPoint curr;

    private final UnitGrid grid;
    private final int grid_size;

    public ShipSlideBehaviour(Ship ship) {
        this.ship = ship;
        grid = ship.getUnitGrid();
        grid_size = grid.getGridSize();

        curr = new ShipTrajectoryPoint(ship);
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append("ShipSlideBehaviour: ");
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

        ShipTrajectoryPoint next = curr.moved(-1.4f * t);

        if (ShipTrajectory.checkShipsCollision(grid, ship, curr, next.moved(-8))) {
            return State.UNINTERRUPTIBLE;
        }

        blocked = false;

        curr = next;

        ship.free();
        ship.setPosition(curr.positionX, curr.positionY);
        ship.setGridPosition(curr.gridX, curr.gridY);
        ship.occupy();

        if (grid.isDeepWater(curr.gridX, curr.gridY)) {
            ship.endSlide();
            return State.DONE;
        }

        return State.UNINTERRUPTIBLE;
    }

    public final boolean isBlocking() {
        return blocked;
    }

    public final void forceInterrupted() {
    }
}
