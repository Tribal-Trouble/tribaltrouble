package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;

import java.util.Iterator;
import java.util.Set;

public final class UnitCountTrigger extends TutorialTrigger {
	private final int target_count;
	
	public UnitCountTrigger(int target_count) {
		super(1f, 0f, "unit_count", new Object[]{target_count});
		this.target_count = target_count;
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
		if (count >= target_count)
			tutorial.done(TutorialForm.TUTORIAL_QUARTERS);
	}
}
