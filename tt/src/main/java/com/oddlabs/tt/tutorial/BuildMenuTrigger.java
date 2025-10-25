package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.player.Player;

public final class BuildMenuTrigger extends TutorialTrigger {
	public BuildMenuTrigger(Player local_player) {
		super(.1f, 0f, "build_menu");
		local_player.enableWeapons(true);
	}

        @Override
	protected void run(Tutorial tutorial) {
		if (tutorial.getViewer().getPanel().inBuildMenu())
			tutorial.next(new WeaponTrigger(tutorial.getViewer().getLocalPlayer()));
	}
}
