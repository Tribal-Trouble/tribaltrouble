package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.MagicFactory;

public final class MagicController extends Controller {
	private final Unit unit;
	private final MagicFactory magic_factory;

	private boolean should_pop = false;

	public MagicController(Unit unit, MagicFactory magic_factory) {
		super(0);
		this.unit = unit;
		this.magic_factory = magic_factory;
	}

	public void popNextTime() {
		should_pop = true;
	}

	@Override
	public void decide() {
		if (should_pop) {
			unit.popController();
		} else {
			unit.setBehaviour(new MagicBehaviour(unit, magic_factory, this));
		}
	}
}
