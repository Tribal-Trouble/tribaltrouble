package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;

public abstract class Accessories extends Model {
	private final SpriteKey sprite_renderer;

	public Accessories(World world, SpriteKey sprite_renderer) {
		super(world);
		this.sprite_renderer = sprite_renderer;
		register();
	}

    @Override
	public final SpriteKey getSpriteRenderer() {
		return sprite_renderer;
	}

    @Override
	public final float getShadowDiameter() {
		return 0f;
	}
}
