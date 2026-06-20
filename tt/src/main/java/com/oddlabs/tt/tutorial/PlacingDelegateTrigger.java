package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.BuildingType;

import com.oddlabs.tt.delegate.PlacingDelegate;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class PlacingDelegateTrigger extends TutorialTrigger {
    public PlacingDelegateTrigger(@NonNull Player player) {
        super(.1f, 0f, "placing");
        player.enableRepairing(false);
        player.enableAttacking(false);
        player.enableBuilding(BuildingType.ARMORY, false);
        player.enableBuilding(BuildingType.TOWER, false);
        player.enableChieftains(false);
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getGUIRoot().getDelegate() instanceof PlacingDelegate)
            tutorial.next(new QuartersTrigger());
    }
}
