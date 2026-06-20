package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.model.UnitType;

import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

/**
 * Tutorial trigger that commands the AI player to attack the player's tower
 * to demonstrate battle mechanics and defense.
 */
public final class AttackTowerTrigger extends TutorialTrigger {
    private static final int NUM_UNITS = 12;

    private final @NonNull Building tower;
    private final @NonNull Player ai;

    public AttackTowerTrigger(@NonNull Building tower) {
        super(.1f, 0f, "attack_tower");
        this.ai = tower.getOwner().getWorld().getPlayers().get(1);
        this.tower = tower;
        Selectable<?>[] units = Selectable.newArray(NUM_UNITS);
        for (int i = 0; i < units.length; i++) {
            units[i] = new Unit(ai, tower.getPositionX() - 50, tower.getPositionY() - 50, null, ai.getRaceInfo()
                    .getUnitTemplate(UnitType.WARRIOR_ROCK));
        }
        ai.setTarget(units, tower, Action.ATTACK, false);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (ai.getUnitCountContainer().getNumSupplies() == 0) {
            tutorial.next(new RepairTowerTrigger(tower));
        }
    }
}
