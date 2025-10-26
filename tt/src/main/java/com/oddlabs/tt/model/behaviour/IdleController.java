package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class IdleController extends Controller {
	private final static float MIN_SCAN_DELAY = 1f;
	private final static float MAX_SCAN_DELAY = 2f;

	private final Unit unit;
	private final AttackScanFilter scan_filter;
	private final @NonNull IdleBehaviour idle_behaviour;
	private final boolean can_move;
	private float redecide_time;

	public IdleController(Unit unit, AttackScanFilter filter, boolean can_move) {
		super(0);
		this.unit = unit;
		this.scan_filter = filter;
		this.idle_behaviour = new IdleBehaviour(this, unit);
		this.can_move = can_move;
	}

	public boolean shouldSleep(float t) {
		redecide_time -= t;
		return redecide_time > 0;
	}

    @Override
	public void decide() {
		unit.setBehaviour(idle_behaviour);
		if (shouldSleep(0f))
			return;
		redecide_time = MIN_SCAN_DELAY + unit.getOwner().getWorld().getRandom().nextFloat()*(MAX_SCAN_DELAY - MIN_SCAN_DELAY);
		if (unit.getAbilities().hasAbilities(Abilities.ATTACK))
			unit.scanVicinity(scan_filter);
        Selectable s = scan_filter.removeTarget();
        if (s != null) {
            if (can_move)
                unit.pushControllers(new Controller[]{new WalkController(unit, new LandscapeTarget(unit.getGridX(), unit.getGridY()), true), new HuntController(unit, s)});
            else
                unit.pushController(new AttackController(unit, s));
        }
	}

    @Override
	public @NonNull String getKey() {
		return super.getKey() + unit.getAbilities().hasAbilities(Abilities.BUILD) + unit.getAbilities().hasAbilities(Abilities.MAGIC);
	}
}
