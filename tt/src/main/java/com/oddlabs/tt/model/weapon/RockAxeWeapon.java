package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class RockAxeWeapon extends RotatingThrowingWeapon {
	private final float ROTS_PER_SECOND = 3;
	private final float ANGLE_DELTA = ROTS_PER_SECOND*360f;
	private final static float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

	public RockAxeWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable target, SpriteKey sprite_renderer, @NonNull Audio throw_sound, Audio[] hit_sounds) {
		super(hit, src, target, sprite_renderer, throw_sound, hit_sounds);
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
		return 1;
	}
}
