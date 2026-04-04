package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class DieBehaviour implements Behaviour {
    private static final float SECONDS_PER_DEATH = 3f;
    private static final float LYING_SECONDS = 1f;
    private static final float MOVING_SECONDS = 60f;

    private static final int MOVING_METERS = 3;

    enum DieState {
        DYING,
        LYING,
        MOVING
    }

    private final @NonNull Unit unit;
    private float anim_time = SECONDS_PER_DEATH;
    private @NonNull DieState state = DieState.DYING;

    private float offset_z = 0;
    private float dz = 0;

    public DieBehaviour(@NonNull Unit unit) {
        this.unit = unit;
        unit.switchAnimation(1f / anim_time, Unit.Animation.DYING);
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time -= t;
        offset_z -= dz * t;
        if (anim_time < 0)
            switchState();
        return State.UNINTERRUPTIBLE;
    }

    private void switchState() {
        switch (state) {
            case DYING -> {
                anim_time += LYING_SECONDS;
                state = DieState.LYING;
            }
            case LYING -> {
                anim_time += MOVING_SECONDS;
                state = DieState.MOVING;
                dz = MOVING_METERS / MOVING_SECONDS;
            }
            case MOVING -> unit.remove();
            default -> {
                assert false;
            }
        }
    }

    public float getOffsetZ() {
        return offset_z;
    }

    @Override
    public boolean isBlocking() {
        throw new IllegalStateException();
    }

    @Override
    public void forceInterrupted() {
    }
}
