package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class HuntController extends Controller {
	private final @NonNull Selectable<?> target;
	private final @NonNull Unit unit;

	public HuntController(@NonNull Unit unit, @NonNull Selectable<?> target) {
		super(1);
		this.unit = unit;
		this.target = target;
	}

	private boolean canAttack() {
		return unit.isCloseEnough(unit.getRange(target), target);
	}

	@Override
	public void decide() {
		if (target.isDead()) {
			unit.popController();
		} else if (canAttack()) {
			unit.setBehaviour(new AttackBehaviour(unit, target));
			resetGiveUpCounter(0);
		} else if (!shouldGiveUp(0)) {
			unit.setBehaviour(new WalkBehaviour(unit, target, unit.getRange(target), false));
		} else
			unit.popController();
	}
}
