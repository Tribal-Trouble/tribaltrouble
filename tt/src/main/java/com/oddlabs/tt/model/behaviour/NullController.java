package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Selectable;

public final class NullController extends Controller {
	private final Selectable selectable;

	public NullController(Selectable s) {
		super(0);
		this.selectable = s;
	}

    @Override
	public String getKey() {
		return super.getKey() + selectable.getAbilities().hasAbilities(Abilities.BUILD_ARMIES) + selectable.getAbilities().hasAbilities(Abilities.REPRODUCE) + selectable.getAbilities().hasAbilities(Abilities.ATTACK);
	}

    @Override
	public void decide() {
		selectable.setBehaviour(new NullBehaviour());
	}
}
