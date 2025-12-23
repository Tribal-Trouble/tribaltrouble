package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AttackScanFilter implements ScanFilter {
    public enum Priority {
        NONE(0),
        QUARTERS(1),
        ARMORY(1),
        TOWER(2),
        PEON(3),
        WARRIOR(4);

        public final int value;

        Priority(int value) {
            this.value = value;
        }
    }

	public static final int UNIT_RANGE = 8;
	public static final int TOWER_RANGE = (int)(RacesResources.THROW_RANGE + MountUnitContainer.ATTACK_RANGE_INCREASE);

	private final int max_range;

	private final @NonNull Player owner;

	private @Nullable Selectable<?> target = null;
	private @NonNull Priority target_priority = Priority.NONE;

	public AttackScanFilter(@NonNull Player owner, int max_range) {
		this.owner = owner;
		this.max_range = max_range;
	}

	public @Nullable Selectable<?> removeTarget() {
		Selectable<?> result = target;
		target = null;
		target_priority = Priority.NONE;
		return result;
	}

	@Override
	public int getMinRadius() {
		return 1;
	}

	@Override
	public int getMaxRadius() {
		return max_range;
	}

	@Override
	public boolean filter(int grid_x, int grid_y, @NonNull Occupant occ) {
		if (occ instanceof Selectable<?> s && owner.isEnemy(s.getOwner())) {
			Priority priority = s.getAttackPriority();
			if (target_priority.value < priority.value) {
				target_priority = priority;
				target = s;
			}
		}
		return false;
	}
}
