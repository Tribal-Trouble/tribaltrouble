package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;

public final class AttackBehaviour implements Behaviour {
	private final static float SECONDS_PER_ATTACK = 2f;

    enum AttackState {
        THROWING,
        RELEASED
    }

	private final Selectable target;
	private final Unit unit;
	private float anim_time;
	private AttackState state;

	public AttackBehaviour(Unit unit, Selectable target) {
		this.unit = unit;
		this.target = target;
		init();
	}

    @Override
	public boolean isBlocking() {
		return true;
	}

    @Override
	public int animate(float t) {
		switch (state) {
			case THROWING:
				updateAttack(t);
				if (anim_time <= 0) {
					if (unit.isMounted())
						unit.getWeaponFactory().attack(unit, target, 3f);
					else
						unit.getWeaponFactory().attack(unit, target);

					anim_time += SECONDS_PER_ATTACK - unit.getWeaponFactory().getSecondsPerRelease(1f/SECONDS_PER_ATTACK);
					state = AttackState.RELEASED;
				}
				return Selectable.UNINTERRUPTIBLE;
			case RELEASED:
				updateAttack(t);
				if (anim_time > 0)
					return Selectable.UNINTERRUPTIBLE;
				else
					return Selectable.DONE;
			default:
				throw new RuntimeException("Invalid state: " + state);
		}
	}

	private void updateAttack(float t) {
		anim_time -= t;
		unit.aimAtTarget(target);
	}

	private void init() {
		state = AttackState.THROWING;
		anim_time += unit.getWeaponFactory().getSecondsPerRelease(1f/SECONDS_PER_ATTACK);
		unit.switchAnimation(1f/SECONDS_PER_ATTACK, Unit.ANIMATION_THROWING);
	}

    @Override
	public void forceInterrupted() {
	}
}
