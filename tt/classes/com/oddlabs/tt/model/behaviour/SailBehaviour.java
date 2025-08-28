package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TrackerAlgorithm;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;

public final strictfp class SailBehaviour implements Behaviour {
    private static final float WAIT_RETRY_DELAY = 1f / 2f;
    private static final float MAX_WAIT_RETRY_DELAY = 5f;

    private final Building boat;
    private final TrackerAlgorithm tracker_algorithm;

    private Movable blocking_movable;
    private int blocker_x;
    private int blocker_y;
    private float retry_delay_counter;
    private float retry_delay;

    private int state;

    public SailBehaviour(Building boat, TrackerAlgorithm tracker_algorithm) {
        this.boat = boat;
        this.tracker_algorithm = tracker_algorithm;
        retry_delay = WAIT_RETRY_DELAY;
        init();
    }

    public SailBehaviour(Building boat, Target t, float range) {
        this(boat, new TargetTrackerAlgorithm(boat.getUnitGrid(), range, t, UnitGrid.SEA));
    }

    public final boolean isBlocking() {
        return state == PathTracker.BLOCKED;
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append("SailBehaviour: state=");
        switch (state) {
            case PathTracker.OK:
                tool_tip_box.append("OK");
                break;
            case PathTracker.OK_INTERRUPTIBLE:
                tool_tip_box.append("OK_INTERRUPTIBLE");
                break;
            case PathTracker.DONE:
                tool_tip_box.append("DONE");
                break;
            case PathTracker.BLOCKED:
                tool_tip_box.append("BLOCKED");
                break;
            case PathTracker.SOFTBLOCKED:
                tool_tip_box.append("SOFTBLOCKED");
                break;
        }
        tool_tip_box.append(" | retry_delay=");
        tool_tip_box.append((int) retry_delay);
        tool_tip_box.append("(");
        tool_tip_box.append((int) retry_delay);
        tool_tip_box.append("s)");
        boat.getTracker().appendToolTip(tool_tip_box);
    }

    private final void switchToMoving() {
        // boat.switchAnimation(unit.getMetersPerSecond(), Unit.ANIMATION_MOVING);
    }

    public final int animate(float t) {
        retry_delay_counter -= t;
        boolean blocker_moved =
                blocking_movable != null
                        && (blocking_movable.getGridX() != blocker_x
                                || blocking_movable.getGridY() != blocker_y);
        if (retry_delay_counter > 0 && !blocker_moved) {
            return Selectable.INTERRUPTIBLE;
        }
        retry_delay_counter = 0;
        blocking_movable = null;
        PathTracker tracker = boat.getTracker();
        state = tracker.animate(4.0f * t);
        boat.setLayer(UnitGrid.SEA);
        switch (state) {
            case PathTracker.OK:
                switchToMoving();
                return Selectable.UNINTERRUPTIBLE;
            case PathTracker.OK_INTERRUPTIBLE:
                retry_delay = WAIT_RETRY_DELAY;
                switchToMoving();
                return Selectable.INTERRUPTIBLE;
            case PathTracker.DONE:
                return Selectable.DONE;
            case PathTracker.BLOCKED: /* fall through */
            case PathTracker.SOFTBLOCKED:
                Occupant blocker = tracker.getBlocker();
                if (blocker instanceof Movable) {
                    blocking_movable = (Movable) blocker;
                    if (!blocking_movable.isDead()) {
                        blocking_movable.markBlocking();
                        blocker_x = blocking_movable.getGridX();
                        blocker_y = blocking_movable.getGridY();
                    } else {
                        blocking_movable = null;
                    }
                }
                retry_delay = StrictMath.min(2 * retry_delay, MAX_WAIT_RETRY_DELAY);
                return doRetry();
            default:
                throw new RuntimeException("Invalid tracker state: " + state);
        }
    }

    private final int doRetry() {
        retry_delay_counter = retry_delay;
        // boat.switchToIdleAnimation();
        return Selectable.INTERRUPTIBLE;
    }

    private final void init() {
        boat.getTracker().setTarget(tracker_algorithm);
    }

    public final void forceInterrupted() {}
}
