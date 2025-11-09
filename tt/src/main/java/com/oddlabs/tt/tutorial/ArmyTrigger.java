package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Set;

public final class ArmyTrigger extends TutorialTrigger {
	private final static int ARMY_SIZE = 10;
	
	public ArmyTrigger(@NonNull Player local_player) {
		super(1f, 0f, "army", new Object[]{ARMY_SIZE});
		local_player.enableMoving(true);
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
		Set<Selectable> set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
		Iterator<Selectable> it = set.iterator();
		int count = 0;
		while (it.hasNext()) {
			Selectable s = it.next();
			if (s instanceof Unit && s.getAbilities().hasAbilities(Abilities.THROW)) {
				count++;
			}
		}
		if (count >= ARMY_SIZE)
			tutorial.done(TutorialForm.TUTORIAL_ARMORY);
	}
}
