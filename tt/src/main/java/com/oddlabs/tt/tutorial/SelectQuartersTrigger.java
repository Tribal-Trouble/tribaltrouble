package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import org.jspecify.annotations.NonNull;

public final class SelectQuartersTrigger extends TutorialTrigger {
    public SelectQuartersTrigger() {
        super(.1f, 0f, "select_quarters");
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        Building building = tutorial.getViewer().getSelection().getCurrentSelection().getBuilding();
        if (building != null && building.getAbilities().hasAbilities(Abilities.REPRODUCE))
            tutorial.next(new UnitsInQuartersTrigger());
    }
}
