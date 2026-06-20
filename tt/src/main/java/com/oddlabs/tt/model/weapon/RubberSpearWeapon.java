package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * A throwing spear weapon made of rubber.
 */
public final class RubberSpearWeapon extends DirectedThrowingWeapon {
    private static final float METERS_PER_SECOND = 30; //multiplied by meters/second (in 2D)
    private static final int MAX_BOUNDS_LENGTH = 3;

    public RubberSpearWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull AudioFile throw_sound, @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit, src, target, throw_sound, hit_sounds);
    }

    @Override
    protected void hitTarget(boolean hit, @NonNull Player owner, @NonNull Selectable<?> target) {
        if (hit)
            damageTarget(target);
        AttackScanFilter filter = new AttackScanFilter(owner, MAX_BOUNDS_LENGTH);
        owner.getWorld().getUnitGrid().scan(filter, target.getGridX(), target.getGridY());
        Selectable<?> s = filter.removeTarget();
        if (s != null && owner.getWorld().getRandom().nextFloat() > .5f) {
            setTarget(s);
        } else
            super.hitTarget(hit, owner, target);
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.RUBBER;
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
