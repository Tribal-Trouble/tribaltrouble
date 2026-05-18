package com.oddlabs.tt.render;


import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class SpriteListRenderer {
    private final @NonNull SpriteList sprite_list;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] @NonNull [] render_lists;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] @NonNull [] respond_render_lists;
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();

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

    private void pickFromList(@NonNull List<@Nullable ModelState<?>> render_list,
            @NonNull List<@NonNull Target> pick_list) {
        for (int i = 0; i < render_list.size(); i++) {
            ModelState<?> model = render_list.get(i);
            render_list.set(i, null);
            if (model.getModel() instanceof Target target) {
                pick_list.add(target);
            }
        }
    }

    public void renderAll(int index, int tex_index) {
        List<ModelState<?>> render_list = render_lists[index][tex_index];
        boolean modulate = sprite_list.getSprite(index).modulateColor();

        for (ModelState<?> modelState : render_list) {
            if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                RenderTools.draw(modelState.getModel());
            }
            // Standard sprites: If modulate, use Blend. If opaque/alpha, use A2C (Blend=False).
            // Depth Write = !modulate (Opaque writes depth, Effects don't).
            instancedSpriteRenderer.add(sprite_list, index, modelState.getModel().getAnimation(),
                    modelState.getModel().getAnimationTicks(), tex_index, false, modulate, !modulate, true,
                    modelState.getTransform(tempMatrix), modelState.getColor(), modelState.getTeamColor());
        }
        render_list.clear();

        render_list = respond_render_lists[index][tex_index];
        if (!render_list.isEmpty()) {
            for (ModelState<?> model : render_list) {
                // Respond color is usually white or specific, here using white as placeholder or model color if needed
                // Respond rendering usually highlights the unit.
                // Using white for color and decal color for now as per original logic which seemed to use default colors.

                if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                // Respond (Overlays) usually shouldn't write depth to avoid z-fighting with the unit itself
                // Let's assume No Depth Write for overlays is safer.
                instancedSpriteRenderer.add(sprite_list, index, model.getModel().getAnimation(),
                        model.getModel().getAnimationTicks(), tex_index, true, true, false, true,
                        model.getTransform(tempMatrix), Color.WHITE, Color.WHITE);
            }
            render_list.clear();
        }
    }
}
