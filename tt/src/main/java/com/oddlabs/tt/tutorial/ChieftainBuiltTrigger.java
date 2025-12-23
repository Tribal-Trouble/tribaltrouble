package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class ChieftainBuiltTrigger extends TutorialTrigger {

    public ChieftainBuiltTrigger() {
		super(.1f, 0f, "chieftain_built");
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
        for (var s : tutorial.getViewer().getLocalPlayer().getUnits().getSet()) {
            if (s instanceof Unit u && u.getAbilities().hasAbilities(Abilities.MAGIC)) {
                tutorial.next(new MagicTrigger(u));
            }
        }
	}
}
