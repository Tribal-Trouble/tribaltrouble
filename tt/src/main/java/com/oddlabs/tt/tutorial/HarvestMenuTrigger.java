package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class HarvestMenuTrigger extends TutorialTrigger {
	public HarvestMenuTrigger(@NonNull Player local_player) {
		super(.1f, 0f, "harvest_menu");
		local_player.enableHarvesting(true);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		if (tutorial.getViewer().getPanel().inHarvestMenu())
			tutorial.next(new SupplyTrigger(tutorial.getViewer().getLocalPlayer()));
	}
}
