package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;

public final class IdleBehaviour implements Behaviour {
	private final IdleController controller;
	private final Unit unit;

	public IdleBehaviour(IdleController controller, Unit unit) {
		this.controller = controller;
		this.unit = unit;
	}

    @Override
	public int animate(float t) {
		unit.switchToIdleAnimation();
		return controller.shouldSleep(t)
                ? Selectable.INTERRUPTIBLE
                : Selectable.DONE;
	}

    @Override
	public boolean isBlocking() {
		return true;
	}

    @Override
	public void forceInterrupted() {
	}
}
