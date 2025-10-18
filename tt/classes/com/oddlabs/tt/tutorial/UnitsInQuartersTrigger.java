package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import java.util.Iterator;
import java.util.Set;

public final class UnitsInQuartersTrigger extends TutorialTrigger {
	public UnitsInQuartersTrigger() {
		super(1f, 0f, "units_in_quarters");
	}

        @Override
	protected void run(Tutorial tutorial) {
		Set set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
		Iterator it = set.iterator();
		int count = 0;
		while (it.hasNext()) {
			Selectable s = (Selectable)it.next();
			if (s instanceof Unit)
				count++;
		}
		if (count == 0)
			tutorial.next(new RallyPointTrigger());
	}
}
