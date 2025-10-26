package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class ArmyMenuTrigger extends TutorialTrigger {
	public ArmyMenuTrigger(@NonNull Player local_player) {
		super(.1f, 1f, "army_menu");
		local_player.enableArmies(true);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		if (tutorial.getViewer().getPanel().inArmyMenu())
			tutorial.next(new ArmyTrigger(tutorial.getViewer().getLocalPlayer()));
	}
}
