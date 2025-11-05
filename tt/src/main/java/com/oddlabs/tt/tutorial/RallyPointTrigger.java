package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class RallyPointTrigger extends TutorialTrigger {
	public RallyPointTrigger() {
		super(1f, 0f, "rally_point");
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		Set<Selectable> set = tutorial.getViewer().getSelection().getCurrentSelection().getSet();
            for (Selectable s : set) {
                if (s instanceof Building b) {
                    if (b.hasRallyPoint())
                        tutorial.next(new UnitCountTrigger(30));
                }
            }
	}
}
