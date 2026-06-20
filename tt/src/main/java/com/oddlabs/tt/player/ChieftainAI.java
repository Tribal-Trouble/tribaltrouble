package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;


/**
 * Abstract base class for the Chieftain AI, implementing base decision-making
 * capabilities and utility computations for player chieftains.
 */
public abstract sealed class ChieftainAI permits NativeChieftainAI, VikingChieftainAI {
    public abstract void decide(Unit chieftain);

    protected final int numEnemyUnits(@NonNull Player owner) {
        return owner.getWorld().getPlayers().stream()
                .filter(owner::isEnemy)
                .mapToInt(p -> p.getUnits().size())
                .sum();
    }
}
