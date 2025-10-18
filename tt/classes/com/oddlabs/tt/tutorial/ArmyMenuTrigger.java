package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.player.Player;

public final class ArmyMenuTrigger extends TutorialTrigger {
	public ArmyMenuTrigger(Player local_player) {
		super(.1f, 1f, "army_menu");
		local_player.enableArmies(true);
	}

        @Override
	protected void run(Tutorial tutorial) {
		if (tutorial.getViewer().getPanel().inArmyMenu())
			tutorial.next(new ArmyTrigger(tutorial.getViewer().getLocalPlayer()));
	}
}
