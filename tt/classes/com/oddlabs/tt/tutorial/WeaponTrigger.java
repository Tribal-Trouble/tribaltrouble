package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;

import java.util.Iterator;
import java.util.Set;

public final class WeaponTrigger extends TutorialTrigger {
	private final static int WEAPONS = 10;

	public WeaponTrigger(Player local_player) {
		super(.5f, 0f, "weapon", new Object[]{WEAPONS});
		local_player.enableHarvesting(true);
	}

        @Override
	protected void run(Tutorial tutorial) {
		Set set = tutorial.getViewer().getSelection().getCurrentSelection().getSet(); 
		Iterator it = set.iterator();
		while (it.hasNext()) {
			Selectable s = (Selectable)it.next();
			if (s instanceof Building && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
				Building armory = (Building)s;
				if (armory.getSupplyContainer(com.oddlabs.tt.model.weapon.RockAxeWeapon.class).getNumSupplies() >= WEAPONS)
					tutorial.next(new ArmyMenuTrigger(tutorial.getViewer().getLocalPlayer()));
			}
		}
	}
}
