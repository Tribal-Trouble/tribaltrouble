package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class ThrowingFactory extends WeaponFactory {
	private static final Class<?>[] WEAPON_CONSTRUCTOR_TYPES = {boolean.class, Unit.class, Selectable.class, SpriteKey.class, Audio.class, Audio[].class};
	private final @NonNull Class<? extends ThrowingWeapon> weapon_type;
    private final @NonNull Constructor<? extends ThrowingWeapon> weapon_constructor;
	private final @NonNull SpriteKey weapon_sprite;
	private final @NonNull Audio throw_sound;
	private final @NonNull Audio @NonNull [] hit_sounds;

	public ThrowingFactory(@NonNull Class<? extends ThrowingWeapon> weapon_type, float hit_chance, float range, float release_ratio, @NonNull SpriteKey weapon_sprite, @NonNull Audio throw_sound, @NonNull Audio @NonNull[] hit_sounds) {
		super(hit_chance, range, release_ratio);
		this.weapon_type = weapon_type;
        try {
            this.weapon_constructor = weapon_type.getConstructor(WEAPON_CONSTRUCTOR_TYPES);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing required constructor from " + weapon_type.getName(), e);
        }
        this.weapon_sprite = weapon_sprite;
		this.throw_sound = throw_sound;
		this.hit_sounds = hit_sounds;
	}

	@Override
	protected void doAttack(boolean hit, @NonNull Unit src, @NonNull Selectable target) {
		try {
            // TODO this is kinda gross
            weapon_constructor.newInstance(hit, src, target, weapon_sprite, throw_sound, hit_sounds);
		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public @NonNull Class<? extends ThrowingWeapon> getType() {
		return weapon_type;
	}
}
