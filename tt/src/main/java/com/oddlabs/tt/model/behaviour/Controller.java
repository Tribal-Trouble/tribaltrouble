package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public abstract sealed class Controller permits AttackController, DefendController, DieController, EnterController,
        GatherController, HarvestController, HuntController, IdleController, MagicController, NullController,
        PlaceBuildingController, RepairController, StunController, TransferUnitController, WalkController {
    private static final int MAX_TRIES = 1;
    private final int @NonNull [] give_up_counters;

    protected Controller(int num_states) {
        give_up_counters = new int[num_states];
    }

    public final void resetGiveUpCounters() {
        Arrays.fill(give_up_counters, 0);
    }

    public final void resetGiveUpCounter(int state_index) {
        give_up_counters[state_index] = 0;
    }

    protected final void resetGiveUpCounter(Enum<?> state) {
        give_up_counters[state.ordinal()] = 0;
    }

    protected final boolean shouldGiveUp(int state_index) {
        if (give_up_counters[state_index] != MAX_TRIES) {
            give_up_counters[state_index]++;
            return false;
        } else {
            return true;
        }
    }

    protected final boolean shouldGiveUp(Enum<?> state) {
        int state_index = state.ordinal();
        if (give_up_counters[state_index] != MAX_TRIES) {
            give_up_counters[state_index]++;
            return false;
        } else {
            return true;
        }
    }

    public @NonNull String getKey() {
        return Integer.toString(getClass().hashCode());
    }

    public abstract void decide();
}
