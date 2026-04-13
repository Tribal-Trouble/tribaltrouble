package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public abstract class DirectedThrowingWeapon extends ThrowingWeapon {
    public DirectedThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target, @NonNull SpriteKey sprite_renderer, @NonNull Audio throw_sound, @NonNull Audio @NonNull [] hit_sounds) {
        super(hit, src, target, sprite_renderer, throw_sound, hit_sounds);
    }

    public float getAngle() {
        return (float) Math.toDegrees(Math.atan2(getZSpeed(), getMetersPerSecond()));
    }

    @Override
    public final void visit(@NonNull ElementVisitor visitor) {
        visitor.visitDirectedThrowingWeapon(this);
    }
}
