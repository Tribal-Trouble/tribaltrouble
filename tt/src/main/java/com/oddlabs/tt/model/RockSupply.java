package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;

public final class RockSupply extends SupplyModel {
	private final static int INITIAL_SUPPLIES = 10;

	public RockSupply(World world, SpriteKey sprite_renderer, float size, int grid_x, int grid_y, float x, float y, float rotation, boolean increase) {
		super(world, sprite_renderer, size, grid_x, grid_y, x, y, rotation, INITIAL_SUPPLIES, increase);
	}

        @Override
	public Supply respawn() {
		return new RockSupply(getWorld(), getSpriteRenderer(), getSize(), getGridX(), getGridY(), getPositionX(), getPositionY(), 0, false);
	}
}
