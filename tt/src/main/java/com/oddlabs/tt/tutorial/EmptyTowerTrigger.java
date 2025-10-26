package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Building;
import org.jspecify.annotations.NonNull;

public final class EmptyTowerTrigger extends TutorialTrigger {
	private final Building tower;
	
	public EmptyTowerTrigger(@NonNull Building tower) {
		super(.1f, 0f, "empty_tower");
		this.tower = tower;
		tower.getOwner().enableTowerExits(true);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		if (tower.getUnitContainer().getNumSupplies() == 0) {
			tutorial.done(TutorialForm.TUTORIAL_TOWER);
		}
	}
}
