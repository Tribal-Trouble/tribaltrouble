package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;

public final class RightPaddle extends SupplyModel {
    public RightPaddle(
            World world,
            SpriteKey sprite_renderer,
            float size,
            int grid_x,
            int grid_y,
            float x,
            float y,
            float rotation,
            boolean increase) {
        super(world, sprite_renderer, size, grid_x, grid_y, x, y, rotation, 1, increase);
    }

    public final Supply respawn() {
        return new RightPaddle(
                getWorld(),
                getSpriteRenderer(),
                getSize(),
                getGridX(),
                getGridY(),
                getPositionX(),
                getPositionY(),
                0,
                false);
    }
}
