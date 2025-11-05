package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class SupplyTrigger extends TutorialTrigger {
	private final static int TREE = 20;
	private final static int ROCK = 10;

	public SupplyTrigger(@NonNull Player player) {
		super(.5f, 0f, "supply", new Object[]{TREE, ROCK});
		player.enableHarvesting(true);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		Set<Selectable> set = tutorial.getViewer().getSelection().getCurrentSelection().getSet();
            for (Selectable s : set) {
                if (s instanceof Building armory && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                    if (armory.getSupplyContainer(com.oddlabs.tt.model.RockSupply.class).getNumSupplies() >= ROCK &&
                            armory.getSupplyContainer(com.oddlabs.tt.landscape.TreeSupply.class).getNumSupplies() >= TREE)
                        tutorial.next(new BuildMenuTrigger(tutorial.getViewer().getLocalPlayer()));
                }
            }

	}
}
