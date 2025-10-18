package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import java.util.Iterator;
import java.util.Set;

public final class QuartersTrigger extends TutorialTrigger {
	public QuartersTrigger() {
		super(1f, 0f, "quarters");
	}

        @Override
	protected void run(Tutorial tutorial) {
		Set set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			Selectable s = (Selectable)it.next();
			if (s instanceof Building)
				tutorial.next(new SelectQuartersTrigger());
		}
	}
}
