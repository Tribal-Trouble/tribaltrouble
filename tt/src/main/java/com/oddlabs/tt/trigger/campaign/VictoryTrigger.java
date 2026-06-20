package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.trigger.IntervalTrigger;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Monitors campaign levels to check for the victory condition, which is triggered
 * when all enemy units are eliminated and the enemy has no active chieftain.
 */
public final class VictoryTrigger extends IntervalTrigger {
    private final @NonNull WorldViewer viewer;
    private final Runnable runnable;

    public VictoryTrigger(@NonNull WorldViewer viewer, Runnable runnable) {
        super(viewer.getWorld(), .5f, 0f);
        this.viewer = viewer;
        this.runnable = runnable;
    }

    @Override
    protected void check() {
        List<@NonNull Player> players = viewer.getWorld().getPlayers();
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
