package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;

import java.util.Iterator;
import java.util.Set;

public final class RallyPointTrigger extends TutorialTrigger {
	public RallyPointTrigger() {
		super(1f, 0f, "rally_point");
	}

        @Override
	protected void run(Tutorial tutorial) {
		Set<Selectable> set = tutorial.getViewer().getSelection().getCurrentSelection().getSet(); 
		Iterator<Selectable> it = set.iterator();
		while (it.hasNext()) {
			Selectable s = it.next();
			if (s instanceof Building) {
				Building b = (Building)s;
				if (b.hasRallyPoint())
					tutorial.next(new UnitCountTrigger(30));
			}
		}
	}
}
