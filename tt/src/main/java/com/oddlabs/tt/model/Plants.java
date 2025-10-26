package com.oddlabs.tt.model;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Plants extends SceneryModel {
	public Plants(World world, float x, float y, float dir_x, float dir_y, SpriteKey sprite_renderer) {
		super(world, x, y, dir_x, dir_y, sprite_renderer);
	}

    @Override
	protected void doRegister() {
		if (Globals.INSERT_PLANTS[Settings.getSettings().graphic_detail]) {
			register();
			reinsert();
		}
	}

    @Override
	public void visit(@NonNull ElementVisitor visitor) {
		visitor.visitPlants(this);
	}
}
