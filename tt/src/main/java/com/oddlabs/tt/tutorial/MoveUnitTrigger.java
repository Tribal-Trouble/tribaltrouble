package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.behaviour.WalkController;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class MoveUnitTrigger extends TutorialTrigger {
	public MoveUnitTrigger(@NonNull Player local_player) {
		super(1f, 2f, "move_unit");
		local_player.enableMoving(true);
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
		for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
			if (s.getPrimaryController() instanceof WalkController) {
				tutorial.done(TutorialForm.TUTORIAL_CAMERA);
			}
		}
	}
}
