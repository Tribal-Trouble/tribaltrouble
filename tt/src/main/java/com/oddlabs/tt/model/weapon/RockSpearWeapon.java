package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class RockSpearWeapon extends DirectedThrowingWeapon {
	private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)
	
	public RockSpearWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target, @NonNull SpriteKey sprite_renderer, @NonNull Audio throw_sound, Audio @NonNull [] hit_sounds) {
		super(hit, src, target, sprite_renderer, throw_sound, hit_sounds);
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
