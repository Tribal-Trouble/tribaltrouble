package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public abstract class RotatingThrowingWeapon extends ThrowingWeapon {
	private float angle = 0;

	public RotatingThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target, @NonNull SpriteKey sprite_renderer, @NonNull Audio throw_sound, Audio @NonNull [] hit_sounds) {
		super(hit, src, target, sprite_renderer, throw_sound, hit_sounds);
	}

	private void setAngle(float angle) {
		this.angle = angle;
	}

	public final float getAngle() {
		return angle;
	}

	@Override
	public final void animate(float t) {
		super.animate(t);
		setAngle(getAngle() + getAngleVelocity()*t);
	}

	protected abstract float getAngleVelocity();

	@Override
	public final void visit(@NonNull ElementVisitor visitor) {
		visitor.visitRotatingThrowingWeapon(this);
	}
}
