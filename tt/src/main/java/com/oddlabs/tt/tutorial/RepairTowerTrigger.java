package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Building;
import org.jspecify.annotations.NonNull;

public final class RepairTowerTrigger extends TutorialTrigger {
	private final @NonNull Building tower;
	
	public RepairTowerTrigger(@NonNull Building tower) {
		super(.1f, 0f, "repair");
		this.tower = tower;
		tower.getOwner().enableRepairing(true);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		if (!tower.isDamaged()) {
			tutorial.next(new EmptyTowerTrigger(tower));
		}
	}
}
