package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.BuildingType;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class BuildingChieftainTrigger extends TutorialTrigger {
    public BuildingChieftainTrigger(@NonNull Player player) {
        super(1f, 0f, "building_chieftain");
        player.enableRepairing(false);
        player.enableAttacking(false);
        //	player.enableQuarters(false);
        player.enableBuilding(BuildingType.ARMORY, false);
        player.enableBuilding(BuildingType.TOWER, false);
        player.enableHarvesting(false);
        player.enableWeapons(false);
        player.enableArmies(false);
        player.enableTransporting(false);
        player.enableRallyPoints(false);
        //	player.enableChieftains(false);
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        for (var s : tutorial.getViewer().getLocalPlayer().getUnits().getSet()) {
            if (s instanceof Building b) {
                b.getChieftainContainer().ifPresent(container -> {
                    if (container.isTraining())
                        tutorial.next(new ChieftainBuiltTrigger());
                });
            }
        }
    }
}
