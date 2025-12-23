package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class WeaponTrigger extends TutorialTrigger {
	private static final int WEAPONS = 10;

	public WeaponTrigger(@NonNull Player local_player) {
		super(.5f, 0f, "weapon", new Object[]{WEAPONS});
		local_player.enableHarvesting(true);
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
		for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
			if (s instanceof Building armory && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
				if (armory.getSupplyContainer(com.oddlabs.tt.model.weapon.RockAxeWeapon.class).getNumSupplies() >= WEAPONS)
					tutorial.next(new ArmyMenuTrigger(tutorial.getViewer().getLocalPlayer()));
			}
		}
	}
}
