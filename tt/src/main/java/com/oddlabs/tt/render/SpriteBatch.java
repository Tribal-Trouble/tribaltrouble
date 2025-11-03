package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

final class SpriteBatch {
	private static final float[] RESPOND_COLOR = {1f, 1f, 1f};
	
	private final List<ModelState> models = new ArrayList<>(256);
	
	void add(@NonNull ModelState model) {
		models.add(model);
	}
	
	void render(@NonNull Sprite sprite, int tex_index, boolean respond, @NonNull SpriteList sprite_list) {
		if (models.isEmpty()) return;
		
		sprite.setup(tex_index, respond, sprite_list);
		
		for (ModelState model : models) {
			if (Globals.isBoundsEnabled(Globals.BOUNDING_PLAYERS))
				RenderTools.draw(model.getModel());
			if (Globals.draw_misc) {
				GL11.glPushMatrix();
				model.transform();
				Sprite.setupDecalColor(respond ? RESPOND_COLOR : model.getTeamColor());
				sprite.render(model.getModel().getAnimation(), model.getModel().getAnimationTicks(), sprite_list);
				GL11.glPopMatrix();
			}
		}
		
		sprite.reset(respond, sprite.modulateColor());
	}
	
	void clear() {
		models.clear();
	}
}
