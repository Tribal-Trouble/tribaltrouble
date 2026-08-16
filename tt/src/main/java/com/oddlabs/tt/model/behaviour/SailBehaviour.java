package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.ShipTrajectory;
import com.oddlabs.tt.pathfinder.ShipTrajectoryPoint;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public final class SailBehaviour implements Behaviour {
    private static final float SHIP_SPEED = 0.45f;
    private ShipTrajectoryPoint next_pose = null;

    private final Ship ship;
    private final Target target;
    private float timer = 0.0f;

    private final ShipTrajectory trajectory;

    private boolean blocked = false;

    public SailBehaviour(Ship ship, Target t) {
        this.ship = ship;
        this.target = t;

        this.trajectory = new ShipTrajectory(ship, t);
    }

    public final boolean isBlocking() {
        return blocked;
    }

    public final ShipTrajectory getTrajectory() {
        return trajectory;
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append("SailBehaviour: ");
        if (blocked) {
            tool_tip_box.append("BLOCKED");
        } else {
            tool_tip_box.append("MOVING");
        }
    }

    private State endTrip() {
        if (!trajectory.isComplete() || !trajectory.almostReachedGoal()) {
            ship.reportStuck();
            return State.INTERRUPTIBLE;
        } else {
            ship.endTrip();
            return State.DONE;
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

        if (!trajectory.exists()) {
            ship.reportStuck();
            return State.INTERRUPTIBLE;
        }

        int rowers = ship.getShipHR().countRowers();
        if (rowers == 0) {
            ship.endTrip();
            return State.DONE;
        }

        if (next_pose == null) {
            float speed = rowers * SHIP_SPEED;
            next_pose = trajectory.advance(speed * t);
        }

        if (trajectory.reachedGoal()) {
            return endTrip();
        }

        ShipTrajectoryPoint fromPoint = new ShipTrajectoryPoint(ship);

        var grid = ship.getUnitGrid();

        if (ShipTrajectory.checkShipsCollision(grid, ship, fromPoint, next_pose.moved(8))) {
            timer += t;
            if (timer >= 0.5f) {
                return endTrip();
            } else {
                return State.UNINTERRUPTIBLE;
            }
        }

        timer = 0.0f;

        ship.free();
        ship.setPosition(next_pose.positionX, next_pose.positionY);
        ship.setGridPosition(next_pose.gridX, next_pose.gridY);
        ship.setDirection(next_pose.directionX, next_pose.directionY);
        next_pose = null;
        ship.occupy();

        return State.UNINTERRUPTIBLE;
    }

    public final void forceInterrupted() {
    }
}
