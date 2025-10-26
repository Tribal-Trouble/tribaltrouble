package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class ReinforcementsTrigger extends IntervalTrigger {
	private final @NonNull Player player;
	private final DeployType type;

	private int units_deployed = 0;

	public ReinforcementsTrigger(@NonNull Player player, DeployType type) {
		super(player.getWorld(), .5f, 0f);
		this.player = player;
		this.type = type;
	}

        @Override
	protected void check() {
		if (player.getArmory() == null) {
			triggered();
		} else if (units_deployed < player.getUnitsLost()) {
			int reinforcements = player.getUnitsLost() - units_deployed;
			if (reinforcements > player.getArmory().getUnitContainer().getNumSupplies()) {
				reinforcements = player.getArmory().getUnitContainer().getNumSupplies();
			}
			if (reinforcements > 0) {
				player.deployUnits(player.getArmory(), type, reinforcements);
				units_deployed += reinforcements;
			}
		}
	}

        @Override
	protected void done() {
	}
}
