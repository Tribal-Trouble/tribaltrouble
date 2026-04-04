package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class StunBehaviour implements Behaviour {
    private final @NonNull StunController controller;
    private final @NonNull Unit unit;

    public StunBehaviour(@NonNull StunController controller, @NonNull Unit unit) {
        this.controller = controller;
        this.unit = unit;
    }

    @Override
    public @NonNull State animate(float t) {
        unit.switchToIdleAnimation();
        return !controller.shouldSleep(t) ? State.DONE : State.UNINTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
