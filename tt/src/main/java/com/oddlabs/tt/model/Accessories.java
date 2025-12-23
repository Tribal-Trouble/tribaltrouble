package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public abstract class Accessories extends Model {
	private final @NonNull SpriteKey sprite_renderer;

	public Accessories(@NonNull World world, @NonNull SpriteKey sprite_renderer) {
		super(world);
		this.sprite_renderer = sprite_renderer;
		register();
	}

	@Override
	public final @NonNull SpriteKey getSpriteRenderer() {
		return sprite_renderer;
	}

	@Override
	public final float getShadowDiameter() {
		return 0f;
	}
}
