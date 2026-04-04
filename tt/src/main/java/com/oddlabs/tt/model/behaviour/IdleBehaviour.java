package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class IdleBehaviour implements Behaviour {
    private final @NonNull IdleController controller;
    private final @NonNull Unit unit;

    public IdleBehaviour(@NonNull IdleController controller, @NonNull Unit unit) {
        this.controller = controller;
        this.unit = unit;
    }

    @Override
    public @NonNull State animate(float t) {
        unit.switchToIdleAnimation();
        return controller.shouldSleep(t) ? State.INTERRUPTIBLE : State.DONE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
