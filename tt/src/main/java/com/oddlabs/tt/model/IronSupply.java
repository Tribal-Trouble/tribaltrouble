package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class IronSupply extends SupplyModel {
	private final static int INITIAL_SUPPLIES = 10;

	public IronSupply(World world, SpriteKey sprite_renderer, float size, int grid_x, int grid_y, float x, float y, float rotation, boolean increase) {
		super(world, sprite_renderer, size, grid_x, grid_y, x, y, rotation, INITIAL_SUPPLIES, increase);
	}

    @Override
	public @NonNull Supply respawn() {
		return new IronSupply(getWorld(), getSpriteRenderer(), getSize(), getGridX(), getGridY(), getPositionX(), getPositionY(), 0, false);
	}
}
