package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Triggers the completion of the current tutorial battle step when
 * all of the target player's units (and potentially buildings) are destroyed.
 */
public final class TutorialOverTrigger extends TutorialTrigger {

    public TutorialOverTrigger() {
        super(1f, 0f, "tutorial_over");
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        List<@NonNull Player> players = tutorial.getViewer().getWorld().getPlayers();
        Player local_player = tutorial.getViewer().getLocalPlayer();

        for (Player current : players) {
            int units = current.getUnitCountContainer().getNumSupplies();
            int buildings = current.getBuildingCountContainer().getNumSupplies();
            if (units == 0 && (current == local_player || buildings == 0)) {
                tutorial.done(TutorialForm.TUTORIAL_BATTLE);
            }
        }
    }
}
