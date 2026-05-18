package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class AttackBehaviour implements Behaviour {
    private static final float SECONDS_PER_ATTACK = 2f;

    enum AttackState {
        THROWING,
        RELEASED
    }

    private final @NonNull Selectable<?> target;
    private final @NonNull Unit unit;
    private float anim_time;
    private @NonNull AttackState state = AttackState.THROWING;

    public AttackBehaviour(@NonNull Unit unit, @NonNull Selectable<?> target) {
        this.unit = unit;
        this.target = target;
        anim_time = unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
        unit.switchAnimation(1f / SECONDS_PER_ATTACK, Unit.Animation.THROWING);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        return switch (state) {
            case THROWING -> {
                updateAttack(t);
                if (anim_time <= 0) {
                    if (unit.isMounted())
                        unit.getWeaponFactory().attack(unit, target, 3f);
                    else
                        unit.getWeaponFactory().attack(unit, target);

                    anim_time += SECONDS_PER_ATTACK - unit.getWeaponFactory().getSecondsPerRelease(
                            1f / SECONDS_PER_ATTACK);
                    state = AttackState.RELEASED;
                }
                yield State.UNINTERRUPTIBLE;
            }
            case RELEASED -> {
                updateAttack(t);
                yield anim_time > 0 ? State.UNINTERRUPTIBLE : State.DONE;
            }
        };
    }

    private void updateAttack(float t) {
        anim_time -= t;
        unit.aimAtTarget(target);
    }

    @Override
    public void forceInterrupted() {
    }
}
