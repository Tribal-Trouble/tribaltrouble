package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.Magic;
import com.oddlabs.tt.model.weapon.MagicFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class MagicBehaviour implements Behaviour {
    private enum MagicState {
        PREPARING,
        CASTING,
        ENDING
    }

    private final @NonNull Unit unit;
    private final @NonNull MagicFactory magic_factory;
    private final @NonNull MagicController controller;
    private @Nullable Magic magic;

    private float anim_time;
    private @NonNull MagicState state = MagicState.PREPARING;

    public MagicBehaviour(@NonNull Unit unit, @NonNull MagicFactory magic_factory,
            @NonNull MagicController controller) {
        this.unit = unit;
        this.magic_factory = magic_factory;
        this.controller = controller;
        anim_time = magic_factory.getSecondsPerInit();
        unit.switchAnimation(1f / magic_factory.getSecondsPerAnim(), Unit.Animation.MAGIC);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time -= t;
        return switch (state) {
            case PREPARING -> {
                if (anim_time <= 0) {
                    state = MagicState.CASTING;
                    magic = magic_factory.execute(unit);
                    anim_time += magic_factory.getSecondsPerRelease() - magic_factory.getSecondsPerInit();
                }
                yield State.UNINTERRUPTIBLE;
            }
            case CASTING -> {
                if (anim_time <= 0) {
                    state = MagicState.ENDING;
                    unit.getOwner().getWorld().getAnimationManagerGameTime().registerAnimation(magic);
                    anim_time += magic_factory.getSecondsPerAnim() - magic_factory.getSecondsPerRelease();
                }
                yield State.UNINTERRUPTIBLE;
            }
            case ENDING -> {
                if (anim_time > 0)
                    yield State.UNINTERRUPTIBLE;
                else {
                    controller.popNextTime();
                    yield State.DONE;
                }
            }
        };
    }

    @Override
    public void forceInterrupted() {
        if (magic != null)
            magic.interrupt();
    }
}
