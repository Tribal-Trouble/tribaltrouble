package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.trigger.IntervalTrigger;
import com.oddlabs.tt.viewer.WorldViewer;

public final class VictoryTrigger extends IntervalTrigger {
	private final WorldViewer viewer;
	private final Runnable runnable;

	public VictoryTrigger(WorldViewer viewer, Runnable runnable) {
		super(viewer.getWorld(), .5f, 0f);
		this.viewer = viewer;
		this.runnable = runnable;
	}

        @Override
	protected void check() {
		Player[] players = viewer.getWorld().getPlayers();
		Player local = viewer.getLocalPlayer();

            for (Player current : players) {
                if (local.isEnemy(current)) {
                    int units = current.getUnitCountContainer().getNumSupplies();
                    if (units > 0 || current.hasActiveChieftain()) {
                        return;
                    }
                }
            }
		triggered();
	}

        @Override
	protected void done() {
		runnable.run();
	}
}
