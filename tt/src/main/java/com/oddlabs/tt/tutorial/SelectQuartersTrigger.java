package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.Abilities;
import org.jspecify.annotations.NonNull;

public final class SelectQuartersTrigger extends TutorialTrigger {
    public SelectQuartersTrigger() {
        super(.1f, 0f, "select_quarters");
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        tutorial.getViewer().getSelection().getCurrentSelection().getBuilding().ifPresent(building -> {
            if (building.getAbilities().hasAbilities(Abilities.REPRODUCE))
                tutorial.next(new UnitsInQuartersTrigger());
        });
    }
}
