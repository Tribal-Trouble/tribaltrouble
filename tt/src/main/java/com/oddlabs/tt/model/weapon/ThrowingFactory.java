package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class ThrowingFactory<W extends ThrowingWeapon> extends WeaponFactory {
    @FunctionalInterface
    public interface WeaponConstructor<W extends ThrowingWeapon> {
        W create(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target, @NonNull SpriteKey sprite_renderer,
                @NonNull Audio throw_sound, Audio @NonNull [] hit_sounds);
    }

    private final @NonNull Class<W> weapon_type;
    private final @NonNull WeaponConstructor<W> weapon_constructor;
    private final @NonNull SpriteKey weapon_sprite;
    private final @NonNull Audio throw_sound;
    private final @NonNull Audio @NonNull [] hit_sounds;

    public ThrowingFactory(@NonNull Class<W> weapon_type, @NonNull WeaponConstructor<W> weapon_constructor,
            float hit_chance, float range, float release_ratio, @NonNull SpriteKey weapon_sprite,
            @NonNull Audio throw_sound, @NonNull Audio @NonNull [] hit_sounds) {
        super(hit_chance, range, release_ratio);
        this.weapon_type = weapon_type;
        this.weapon_constructor = weapon_constructor;
        this.weapon_sprite = weapon_sprite;
        this.throw_sound = throw_sound;
        this.hit_sounds = hit_sounds;
    }

    @Override
    protected void doAttack(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        weapon_constructor.create(hit, src, target, weapon_sprite, throw_sound, hit_sounds);
    }

    @Override
    public @NonNull Class<W> getType() {
        return weapon_type;
    }
}
