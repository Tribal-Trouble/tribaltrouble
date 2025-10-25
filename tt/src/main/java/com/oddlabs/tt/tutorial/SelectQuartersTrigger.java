package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;

public final class SelectQuartersTrigger extends TutorialTrigger {
	public SelectQuartersTrigger() {
		super(.1f, 0f, "select_quarters");
	}

        @Override
	protected void run(Tutorial tutorial) {
		Building building = tutorial.getViewer().getSelection().getCurrentSelection().getBuilding();
		if (building != null && building.getAbilities().hasAbilities(Abilities.REPRODUCE))
			tutorial.next(new UnitsInQuartersTrigger());
	}
}
