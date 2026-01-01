package com.oddlabs.tt.render;

import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class SpriteRenderer {
	private final @NonNull SpriteList sprite_list;
	private final @NonNull SpriteListRenderer sprite_list_renderer;
	private final int tex_index;
	private final List<@NonNull ModelState<?>> no_detail_render_list = new ArrayList<>();
	private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();

	public SpriteRenderer(@NonNull SpriteList sprite_list, int tex_index, @NonNull InstancedSpriteRenderer spriteRenderer) {
		this.sprite_list = sprite_list;
		this.tex_index = tex_index;
		this.instancedSpriteRenderer = spriteRenderer;
		sprite_list_renderer = new SpriteListRenderer(sprite_list, spriteRenderer);
	}

	public @NonNull SpriteList getSpriteList() {
		return sprite_list;
	}

	void addToNoDetailList(@NonNull ModelState<?> model) {
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
		for (ModelState<?> model : no_detail_render_list) {
			pick_list.add((Target) model.getModel());
		}
		clearRenderLists();
	}

	public void renderAll() {
		for (int i = 0; i < sprite_list.getNumSprites(); i++) {
			sprite_list_renderer.renderAll(i, tex_index);
		}
	}

	public void renderNoDetail() {
		if (Globals.draw_misc && !no_detail_render_list.isEmpty()) {
            SpriteList quadList = SpriteList.getQuadInstance();
			for (var model : no_detail_render_list) {
                if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
				float x = model.getModel().getPositionX();
				float y = model.getModel().getPositionY();
				float z = model.getModel().getPositionZ();
				float r = model.getModel().getNoDetailSize();
				tempMatrix.identity().translation(x, y, z + 0.1f).scale(r * 2);
				// Quads don't have animation, so pass 0, 0f
                // Disable depth test for no-detail sprites (overlays)
				instancedSpriteRenderer.add(quadList, 0, 0, 0f, 0, false, false, tempMatrix, model.getTeamColor(), Color.TRANSPARENT);
			}
		}
		clearRenderLists();
	}
}
