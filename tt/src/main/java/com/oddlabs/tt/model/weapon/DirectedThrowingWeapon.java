package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * A base class for weapons that are thrown in a specific direction (e.g., spears).
 */
public abstract sealed class DirectedThrowingWeapon extends ThrowingWeapon permits RockSpearWeapon, IronSpearWeapon,
        RubberSpearWeapon {
    public DirectedThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull AudioFile throw_sound, @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit, src, target, throw_sound, hit_sounds);
    }

    public float getAngle() {
        return (float) Math.toDegrees(Math.atan2(getZSpeed(), getMetersPerSecond()));
    }

    @Override
    protected float getLoftFactor() {
        return 1.05f;
    }
}
