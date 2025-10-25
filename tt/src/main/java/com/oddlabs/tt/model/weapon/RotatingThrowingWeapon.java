package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;

public abstract class RotatingThrowingWeapon extends ThrowingWeapon {
	private float angle = 0;

	public RotatingThrowingWeapon(boolean hit, Unit src, Selectable target, SpriteKey sprite_renderer, Audio throw_sound, Audio[] hit_sounds) {
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
	public final void visit(ElementVisitor visitor) {
		visitor.visitRotatingThrowingWeapon(this);
	}
}
