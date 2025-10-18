package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Building;

public final class EmptyTowerTrigger extends TutorialTrigger {
	private final Building tower;
	
	public EmptyTowerTrigger(Building tower) {
		super(.1f, 0f, "empty_tower");
		this.tower = tower;
		tower.getOwner().enableTowerExits(true);
	}

        @Override
	protected void run(Tutorial tutorial) {
		if (tower.getUnitContainer().getNumSupplies() == 0) {
			tutorial.done(TutorialForm.TUTORIAL_TOWER);
		}
	}
}
