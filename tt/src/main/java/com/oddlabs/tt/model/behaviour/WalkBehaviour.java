package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.pathfinder.TrackerAlgorithm;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class WalkBehaviour implements Behaviour {
	private static final float WAIT_RETRY_DELAY = 1f/2f;
	private static final float MAX_WAIT_RETRY_DELAY = 5f;

	private final @NonNull Unit unit;
	private final @NonNull TrackerAlgorithm tracker_algorithm;
	private final @NonNull AttackScanFilter scan_filter;
	private final boolean scan_attack;

	private @Nullable Movable blocking_movable;
	private int blocker_x;
	private int blocker_y;
	private float retry_delay_counter;
	private float retry_delay;

	private PathTracker.State state;

	public WalkBehaviour(@NonNull Unit unit, @NonNull TrackerAlgorithm tracker_algorithm, boolean scan_attack) {
		this.unit = unit;
		this.tracker_algorithm = tracker_algorithm;
		this.scan_attack = scan_attack;
		scan_filter = new AttackScanFilter(unit.getOwner(), AttackScanFilter.UNIT_RANGE);
		retry_delay = WAIT_RETRY_DELAY;
        unit.getTracker().setTarget(tracker_algorithm);
	}

	public WalkBehaviour(@NonNull Unit unit, @NonNull Target t, float range, boolean scan_attack) {
		this(unit, new TargetTrackerAlgorithm(unit.getUnitGrid(), range, t), scan_attack);
	}

	@Override
	public boolean isBlocking() {
		return state == PathTracker.State.BLOCKED;
	}

	public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
		tool_tip_box.append("WalkBehaviour: state=");
        tool_tip_box.append(state.toString());
		tool_tip_box.append(" | retry_delay=");
		tool_tip_box.append((long)retry_delay);
		tool_tip_box.append("(");
		tool_tip_box.append((long)retry_delay);
		tool_tip_box.append("s)");
		unit.getTracker().appendToolTip(tool_tip_box);
	}

	private void switchToMoving() {
		unit.switchAnimation(unit.getMetersPerSecond(), Unit.Animation.MOVING);
	}

	@Override
	public @NonNull State animate(float t) {
		retry_delay_counter -= t;
		boolean blocker_moved = blocking_movable != null && (blocking_movable.getGridX() != blocker_x || blocking_movable.getGridY() != blocker_y);
		if (retry_delay_counter > 0 && !blocker_moved) {
			return State.INTERRUPTIBLE;
		}
		retry_delay_counter = 0;
		blocking_movable = null;
		PathTracker tracker = unit.getTracker();
		state = tracker.animate(unit.getMetersPerSecond()*t);
        return switch (state) {
            case OK -> {
                switchToMoving();
                yield State.UNINTERRUPTIBLE;
            }
            case OK_INTERRUPTIBLE -> {
                retry_delay = WAIT_RETRY_DELAY;
                switchToMoving();
                scan();
                yield State.INTERRUPTIBLE;
            }
            case DONE -> State.DONE;
            case BLOCKED, SOFTBLOCKED -> {
                Occupant blocker = tracker.getBlocker();
                if (blocker instanceof Movable movable) {
                    blocking_movable = movable;
                    if (!blocking_movable.isDead()) {
                        blocking_movable.markBlocking();
                        blocker_x = blocking_movable.getGridX();
                        blocker_y = blocking_movable.getGridY();
                    } else {
                        blocking_movable = null;
                    }
                }
                scan();
                retry_delay = Math.min(2 * retry_delay, MAX_WAIT_RETRY_DELAY);
                yield doRetry();
            }
        };
	}

	private void scan() {
		if (scan_attack) {
			unit.scanVicinity(scan_filter);
			Selectable<?> s = scan_filter.removeTarget();
			if (s != null) {
				unit.getCurrentController().resetGiveUpCounters();
				unit.pushController(new HuntController(unit, s));
			}
		}
	}

/*	public final void moveNextAnimate() {
		retry_delay_counter = 0f;
	}
*/
	private @NonNull State doRetry() {
		retry_delay_counter = retry_delay;
		unit.switchToIdleAnimation();
		return State.INTERRUPTIBLE;
	}

	@Override
	public void forceInterrupted() {
	}
}
