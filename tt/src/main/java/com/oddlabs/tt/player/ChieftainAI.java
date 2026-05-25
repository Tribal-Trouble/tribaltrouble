package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public abstract class ChieftainAI {
    public abstract void decide(Unit chieftain);

    protected final int numEnemyUnits(@NonNull Player owner) {
        Player[] players = owner.getWorld().getPlayers();
        int count = Arrays.stream(players).filter(owner::isEnemy).mapToInt(p -> p.getUnits().size()).sum();
        return count;
    }
}
