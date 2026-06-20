package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * A factory for creating throwing weapons.
 *
 * @param <W> the type of throwing weapon produced by this factory
 */
public final class ThrowingFactory<W extends ThrowingWeapon> extends WeaponFactory {
    @FunctionalInterface
    public interface WeaponConstructor<W extends ThrowingWeapon> {
        W create(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
                @NonNull AudioFile throw_sound, AudioFile @NonNull [] hit_sounds);
    }

    private final @NonNull Class<W> weapon_type;
    private final @NonNull WeaponConstructor<W> weapon_constructor;
    private final @NonNull AudioFile throw_sound;
    private final @NonNull AudioFile @NonNull [] hit_sounds;

    public ThrowingFactory(@NonNull Class<W> weapon_type, @NonNull WeaponConstructor<W> weapon_constructor,
            float hit_chance, float range, float release_ratio, @NonNull AudioFile throw_sound,
            @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit_chance, range, release_ratio);
        this.weapon_type = weapon_type;
        this.weapon_constructor = weapon_constructor;
        this.throw_sound = throw_sound;
        this.hit_sounds = hit_sounds;
    }

    @Override
    protected void doAttack(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        weapon_constructor.create(hit, src, target, throw_sound, hit_sounds);
    }

    @Override
    public @NonNull Optional<Class<? extends ThrowingWeapon>> getType() {
        return Optional.of(weapon_type);
    }
}
