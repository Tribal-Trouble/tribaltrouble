package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

public final class NullBehaviour implements Behaviour {
    @Override
    public @NonNull State animate(float t) {
        return State.INTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
