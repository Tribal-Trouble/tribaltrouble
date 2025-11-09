package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class MagicTrigger extends TutorialTrigger {
	private final boolean[] magic_used = new boolean[RacesResources.NUM_MAGIC];
	
	private final Unit chieftain;
	
	public MagicTrigger(Unit chieftain) {
		super(.1f, 20f, "magic");
		this.chieftain = chieftain;
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
		int last = chieftain.getLastMagicIndex();
		if (last != -1)
			magic_used[last] = true;
            for (boolean b : magic_used) {
                if (!b)
                    return;
            }
		tutorial.done(TutorialForm.TUTORIAL_CHIEFTAIN);
	}
}
