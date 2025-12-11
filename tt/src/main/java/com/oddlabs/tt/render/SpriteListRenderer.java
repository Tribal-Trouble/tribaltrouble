package com.oddlabs.tt.render;


import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.SpriteBatchRenderer;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

final class SpriteListRenderer {
	private final @NonNull SpriteList sprite_list;
	private final List<ModelState<?>>@NonNull [] @NonNull [] render_lists;
	private final List<ModelState<?>> @NonNull [] @NonNull [] respond_render_lists;
	private final @NonNull SpriteBatch batch;

	@SuppressWarnings("unchecked")
	SpriteListRenderer(@NonNull SpriteList sprite_list, @NonNull SpriteBatchRenderer spriteBatchRenderer) {
		this.sprite_list = sprite_list;
        this.batch = new SpriteBatch(spriteBatchRenderer);
		int num_sprites = sprite_list.getNumSprites();
		render_lists = (List<ModelState<?>>[][]) new ArrayList<?>[num_sprites][];
		respond_render_lists = (List<ModelState<?>>[][]) new ArrayList[num_sprites][];
		for (int i = 0; i < num_sprites; i++) {
			Sprite sprite = sprite_list.getSprite(i);
			render_lists[i] = (List<ModelState<?>>[]) new ArrayList<?>[sprite.getNumTextures()];
			respond_render_lists[i] = (List<ModelState<?>>[]) new ArrayList<?>[sprite.getNumTextures()];
			for (int j = 0; j < render_lists[i].length; j++) {
				render_lists[i][j] = new ArrayList<>();
				respond_render_lists[i][j] = new ArrayList<>();
			}
		}
	}

	public void addToRenderList(ModelState<?> model, int sprite_index, int tex_index) {
		render_lists[sprite_index][tex_index].add(model);
	}

	public void addToRespondRenderList(ModelState<?> model, int sprite_index, int tex_index) {
		respond_render_lists[sprite_index][tex_index].add(model);
	}

	public void getAllPicks(@NonNull List<Target> pick_list, int sprite_index, int tex_index) {
		List<ModelState<?>> render_list = render_lists[sprite_index][tex_index];
		pickFromList(render_list, pick_list);
		render_list.clear();

		render_list = respond_render_lists[sprite_index][tex_index];
		pickFromList(render_list, pick_list);
		render_list.clear();
	}

	private void pickFromList(@NonNull List<ModelState<?>> render_list, @NonNull List<Target> pick_list) {
		for (int i = 0; i < render_list.size(); i++) {
			ModelState<?> model = render_list.get(i);
			render_list.set(i, null);
			pick_list.add((Target)model.getModel());
		}
	}

	public void renderAll(@NonNull SpriteShader shader, int index, int tex_index, @NonNull CameraState cameraState) {
		List<ModelState<?>> render_list = render_lists[index][tex_index];
		Sprite sprite = sprite_list.getSprite(index);
		
		batch.clear();
		for (ModelState<?> model : render_list) {
			batch.add(model);
		}
		batch.render(shader, sprite, tex_index, false, sprite_list, cameraState);
		render_list.clear();

		render_list = respond_render_lists[index][tex_index];
		if (!render_list.isEmpty()) {
			batch.clear();
			for (ModelState<?> model : render_list) {
				batch.add(model);
			}
			batch.render(shader, sprite, tex_index, true, sprite_list, cameraState);
			render_list.clear();
		}
	}

	public void debugRender() {
		batch.debugRender();
	}
}
