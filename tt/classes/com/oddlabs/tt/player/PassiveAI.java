package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.behaviour.IdleController;
import com.oddlabs.tt.util.Target;

public final class PassiveAI extends AI {
	private final boolean walk_around;

	public PassiveAI(Player owner, UnitInfo unit_info, boolean walk_around) {
		super(owner, unit_info);
		this.walk_around = walk_around;
	}

    @Override
	public void animate(float time) {
		if (walk_around) {
			if (!shouldDoAction(time))
				return;
			Selectable[][] lists = getOwner().classifyUnits();

                    for (Selectable[] list : lists) {
                        Selectable s = list[0];
                        if (s.getPrimaryController() instanceof IdleController) {
                            for (Selectable thrower : list) {
                                float r = getOwner().getWorld().getRandom().nextFloat();
                                if (r < .2) {
                                    Target walkable_target = getTarget(getOwner().getWorld().getRandom());
                                    getOwner().setTarget(new Selectable[]{thrower}, walkable_target, Target.ACTION_ATTACK, true);
                                }
                            }
                        }
                    }
			if (getOwner().hasActiveChieftain()) {
				getOwner().getRace().getChieftainAI().decide(getOwner().getChieftain());
			}
		}
	}
}
