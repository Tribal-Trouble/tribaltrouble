package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

/**
 * Plants scenery model, representing ground vegetation.
 */
public final class Plants extends SceneryModel {
    public Plants(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundsProvider boundsProvider) {
        super(world, x, y, dir_x, dir_y, boundsProvider);
        world.registerPlant(this);
    }


    @Override
    protected void doRegister() {
        register();
        reinsert();
    }
}
