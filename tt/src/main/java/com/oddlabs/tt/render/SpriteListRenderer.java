package com.oddlabs.tt.render;


import com.oddlabs.tt.util.Target;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

final class SpriteListRenderer {
	private final @NonNull SpriteList sprite_list;
	private final List<ModelState<?>>@NonNull [] @NonNull [] render_lists;
	private final List<ModelState<?>> @NonNull [] @NonNull [] respond_render_lists;
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();
    private final float[] tempColor = new float[4];
    private final float[] tempDecalColor = new float[4];
    
    @SuppressWarnings("unchecked")
	SpriteListRenderer(@NonNull SpriteList sprite_list, @NonNull InstancedSpriteRenderer instancedSpriteRenderer) {
		this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
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
			if (model.getModel() instanceof Target) {
				pick_list.add((Target)model.getModel());
			}
		}
	}
    
    public void renderAll(int index, int tex_index) {
        List<ModelState<?>> render_list = render_lists[index][tex_index];
		
        for (ModelState<?> modelState : render_list) {
            modelState.getTransform(tempMatrix);
            Vector4f colorVec = modelState.getColor();
            tempColor[0] = colorVec.x;
            tempColor[1] = colorVec.y;
            tempColor[2] = colorVec.z;
            tempColor[3] = colorVec.w;
            
            float[] teamColor = modelState.getTeamColor();
            if (teamColor.length >= 3) {
                tempDecalColor[0] = teamColor[0];
                tempDecalColor[1] = teamColor[1];
                tempDecalColor[2] = teamColor[2];
                tempDecalColor[3] = 1.0f;
            } else {
                tempDecalColor[0] = 1.0f;
                tempDecalColor[1] = 1.0f;
                tempDecalColor[2] = 1.0f;
                tempDecalColor[3] = 1.0f;
            }

            instancedSpriteRenderer.add(sprite_list, index, modelState.getModel().getAnimation(), modelState.getModel().getAnimationTicks(), tex_index, false, true, tempMatrix, tempColor, tempDecalColor);
        }
		render_list.clear();

		render_list = respond_render_lists[index][tex_index];
		if (!render_list.isEmpty()) {
			for (ModelState<?> model : render_list) {
                model.getTransform(tempMatrix);
                
                // Respond color is usually white or specific, here using white as placeholder or model color if needed
                // Respond rendering usually highlights the unit.
                // Using white for color and decal color for now as per original logic which seemed to use default colors.
                
                instancedSpriteRenderer.add(sprite_list, index, model.getModel().getAnimation(), model.getModel().getAnimationTicks(), tex_index, true, true, tempMatrix, new float[]{1f, 1f, 1f, 1f}, new float[]{1f, 1f, 1f, 1f});
			}
			render_list.clear();
		}
    }

	public void debugRender() {
        // TODO: Re-implement debug rendering
	}
}
