package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class SelectArmoryTrigger extends TutorialTrigger {
	public SelectArmoryTrigger(@NonNull Player player) {
		super(.1f, 0f, "select_armory");
		player.enableRepairing(false);
		player.enableAttacking(false);
		player.enableBuilding(Race.BUILDING_QUARTERS, false);
		player.enableBuilding(Race.BUILDING_TOWER, false);
		player.enableHarvesting(false);
		player.enableWeapons(false);
		player.enableArmies(false);
		player.enableTransporting(false);
		player.enableRallyPoints(false);
		player.enableChieftains(false);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		Building building = tutorial.getViewer().getSelection().getCurrentSelection().getBuilding();
		if (building != null && building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES))
			tutorial.next(new HarvestMenuTrigger(tutorial.getViewer().getLocalPlayer()));
	}
}
