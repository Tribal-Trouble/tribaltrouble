package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;

public final class StunController extends Controller {
	private final Unit unit;
	private final StunBehaviour stun_behaviour;

	private float time;

	public StunController(Unit unit, float time) {
		super(0);
		this.unit = unit;
		this.time = time;
		stun_behaviour = new StunBehaviour(this, unit);
	}

	public boolean shouldSleep(float t) {
		time -= t;
		return time > 0;
	}

    @Override
	public void decide() {
		unit.setBehaviour(stun_behaviour);
		if (!shouldSleep(0f)) {
			unit.popController();
		}
	}
}
