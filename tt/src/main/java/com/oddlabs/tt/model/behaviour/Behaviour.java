package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

/**
 * Represents a logical state or action of a world entity.
 * Handled via a state machine within the model's update loop.
 */
public sealed interface Behaviour permits AttackBehaviour, DieBehaviour, HarvestBehaviour, IdleBehaviour,
        MagicBehaviour, NullBehaviour, RepairBehaviour, StunBehaviour, WalkBehaviour {
    enum State {
        UNINTERRUPTIBLE,
        INTERRUPTIBLE,
        DONE
    }

    @NonNull
    State animate(float t);

    boolean isBlocking();

    void forceInterrupted();

    default void onCleanup() {
    }
}
