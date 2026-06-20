package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * A throwing axe weapon made of iron.
 */
public final class IronAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 6;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final float METERS_PER_SECOND = 25f; //multiplied by meters/second (in 2D)

    public IronAxeWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull AudioFile throw_sound,
            @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit, src, target, throw_sound, hit_sounds);
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.IRON;
    }

    @Override
    protected float getAngleVelocity() {
        return ANGLE_DELTA;
    }

    @Override
    protected float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 2;
    }
}
