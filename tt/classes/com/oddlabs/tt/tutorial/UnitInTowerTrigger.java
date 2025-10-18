package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Building;

public final class UnitInTowerTrigger extends TutorialTrigger {
	private final Building tower;
	
	public UnitInTowerTrigger(Building tower) {
		super(.1f, 0f, "unit_in_tower");
		this.tower = tower;
	}

        @Override
	protected void run(Tutorial tutorial) {
		if (tower.getUnitContainer().getNumSupplies() > 0) {
			tutorial.next(new AttackTowerTrigger(tower));
		}
	}
}
