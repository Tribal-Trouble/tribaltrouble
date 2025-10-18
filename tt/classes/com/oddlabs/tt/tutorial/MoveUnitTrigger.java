package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.behaviour.WalkController;
import com.oddlabs.tt.player.Player;

import java.util.Iterator;
import java.util.Set;

public final class MoveUnitTrigger extends TutorialTrigger {
	public MoveUnitTrigger(Player local_player) {
		super(1f, 2f, "move_unit");
		local_player.enableMoving(true);
	}

        @Override
	protected void run(Tutorial tutorial) {
		Set set = tutorial.getViewer().getSelection().getCurrentSelection().getSet(); 
		Iterator it = set.iterator();
		while (it.hasNext()) {
			Selectable s = (Selectable)it.next();
			if (s.getPrimaryController() instanceof WalkController) {
				tutorial.done(TutorialForm.TUTORIAL_CAMERA);
			}
		}
	}
}
