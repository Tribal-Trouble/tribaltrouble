package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * A throwing axe weapon made of rock.
 */
public final class RockAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 3;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

    public RockAxeWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull AudioFile throw_sound,
            @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit, src, target, throw_sound, hit_sounds);
    }

    @Override
    protected float getAngleVelocity() {
        return ANGLE_DELTA;
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.ROCK;
    }

    @Override
    protected float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 1;
    }
}
