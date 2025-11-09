package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;

public final class AttackController extends Controller {

	private final Selectable target;
	private final Unit unit;

	public AttackController(Unit unit, Selectable target) {
		super(0);
		this.unit = unit;
		this.target = target;
	}

	private boolean canAttack() {
		return unit.isCloseEnough(unit.getRange(target), target);
	}

	@Override
	public void decide() {
		if (target.isDead() || !canAttack()) {
			unit.popController();
		} else {
			unit.setBehaviour(new AttackBehaviour(unit, target));
		}
	}
}
