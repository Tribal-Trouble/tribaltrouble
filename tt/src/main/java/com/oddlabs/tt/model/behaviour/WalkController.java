package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public final class WalkController extends Controller {
	private final @NonNull Unit unit;
	private final @NonNull Target target;
	private final boolean scan_attack;

	public WalkController(@NonNull Unit unit, @NonNull Target t, boolean scan_attack) {
		super(1);
		this.unit = unit;
		this.target = t;
		this.scan_attack = scan_attack;
	}

	@Override
	public void decide() {
		if (shouldGiveUp(0))
			unit.popController();
		else
			unit.setBehaviour(new WalkBehaviour(unit, target, 0f, scan_attack));
	}

	public boolean isAgressive() {
		return scan_attack;
	}

	public Target getTarget() {
		return target;
	}
}
