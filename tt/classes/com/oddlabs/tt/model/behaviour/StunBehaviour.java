package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;

public final class StunBehaviour implements Behaviour {
	private final StunController controller;
	private final Unit unit;

	public StunBehaviour(StunController controller, Unit unit) {
		this.controller = controller;
		this.unit = unit;
	}

    @Override
	public int animate(float t) {
		unit.switchToIdleAnimation();
		if (!controller.shouldSleep(t))
			return Selectable.DONE;
		else
			return Selectable.UNINTERRUPTIBLE;
	}

    @Override
	public boolean isBlocking() {
		return true;
	}

    @Override
	public void forceInterrupted() {
	}
}
