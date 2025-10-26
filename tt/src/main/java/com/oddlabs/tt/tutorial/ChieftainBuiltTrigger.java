package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class ChieftainBuiltTrigger extends TutorialTrigger {
	private @Nullable Unit chieftain = null;
	
	public ChieftainBuiltTrigger() {
		super(.1f, 0f, "chieftain_built");
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		Set<Selectable> set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
            for (Selectable s : set) {
                if (s instanceof Unit) {
                    Unit u = (Unit) s;
                    if (u.getAbilities().hasAbilities(Abilities.MAGIC)) {
                        chieftain = u;
                        tutorial.next(new MagicTrigger(chieftain));
                    }
                }
            }
	}
}
