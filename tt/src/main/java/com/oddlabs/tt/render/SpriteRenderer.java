package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.SpriteBatchRenderer;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final class SpriteRenderer {
	private final @NonNull SpriteList sprite_list;
	private final @NonNull SpriteListRenderer sprite_list_renderer;
	private final int tex_index;
	private final List<@NonNull ModelState<?>> no_detail_render_list = new ArrayList<>();
	private final @NonNull SpriteBatchRenderer spriteBatchRenderer;

	public SpriteRenderer(@NonNull SpriteList sprite_list, int tex_index, @NonNull SpriteBatchRenderer spriteBatchRenderer) {
		this.sprite_list = sprite_list;
		this.tex_index = tex_index;
		this.spriteBatchRenderer = spriteBatchRenderer;
		sprite_list_renderer = new SpriteListRenderer(sprite_list, spriteBatchRenderer);
	}

	public @NonNull SpriteList getSpriteList() {
		return sprite_list;
	}



	void addToNoDetailList(ModelState<?> model) {
		no_detail_render_list.add(model);
	}

	void addToRenderList(@NonNull PolyDetail detail, ModelState<?> model, boolean respond) {
        int index = detail.ordinal();
		index = Math.min(sprite_list.getNumSprites() - 1, index);
		if (respond) {
			sprite_list_renderer.addToRespondRenderList(model, index, tex_index);
		} else {
			sprite_list_renderer.addToRenderList(model, index, tex_index);
		}
	}

	int getTriangleCount(@NonNull PolyDetail detail) {
        int index = detail.ordinal();
		index = Math.min(sprite_list.getNumSprites() - 1, index);
		return sprite_list.getSprite(index).getTriangleCount();
	}


	private void clearRenderLists() {
		no_detail_render_list.clear();
	}

	public void getAllPicks(@NonNull List<@NonNull Target> pick_list) {
		for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            sprite_list_renderer.getAllPicks(pick_list, i, tex_index);
        }
		for (int i = 0; i < no_detail_render_list.size(); i++) {
			var model = no_detail_render_list.get(i);
			no_detail_render_list.set(i, null);
			pick_list.add((Target)model.getModel());
		}
		clearRenderLists();
	}

	public void renderAll(@NonNull SpriteShader shader, @NonNull CameraState camera_state) {
		for (int i = 0; i < sprite_list.getNumSprites(); i++) {
			sprite_list_renderer.renderAll(shader, i, tex_index, camera_state);
		}
	}

	public void renderNoDetail() {
		if (!no_detail_render_list.isEmpty()) {
			spriteBatchRenderer.begin(null, false, false);
			try {
				for (var model : no_detail_render_list) {
					if (Globals.draw_misc) {
						float[] color = model.getTeamColor();
						float x = model.getModel().getPositionX();
						float y = model.getModel().getPositionY();
						float z = model.getModel().getPositionZ();
						float r = model.getModel().getNoDetailSize();
						spriteBatchRenderer.drawQuad(x - r, y - r, z + 0.1f, r * 2, r * 2, color[0], color[1], color[2], 1f, 0, 0, 0, 0);
					}
				}
			} finally {
				spriteBatchRenderer.end();
				GL11.glDepthMask(true);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
		}
		clearRenderLists();
	}

	public void debugRender() {
		sprite_list_renderer.debugRender();
		if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
			for (ModelState<?> model : no_detail_render_list) {
				RenderTools.draw(model.getModel());
			}
		}
	}
}
