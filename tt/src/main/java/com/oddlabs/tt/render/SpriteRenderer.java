package com.oddlabs.tt.render;

import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Target;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the geometry and textures for a single sprite type.
 */
public final class SpriteRenderer {
    private final @NonNull SpriteList sprite_list;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] render_lists;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] respond_render_lists;
    private final @NonNull Texture @NonNull [] textures;
    private final @Nullable Texture @NonNull [] team_textures;
    private final @Nullable Texture @NonNull [] bump_textures;
    private final List<@NonNull ModelState<?>> no_detail_render_list = new ArrayList<>();
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();

    @SuppressWarnings("unchecked")
    SpriteRenderer(@NonNull SpriteList sprite_list, int tex_index,
            @NonNull InstancedSpriteRenderer instancedSpriteRenderer) {
        this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;

        int numSprites = sprite_list.getNumSprites();
        this.textures = new Texture[numSprites];
        this.team_textures = new Texture[numSprites];
        this.bump_textures = new Texture[numSprites];

        for (int i = 0; i < numSprites; i++) {
            Sprite sprite = sprite_list.getSprite(i);
            textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_NORMAL];
            team_textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_TEAM];
            if (sprite.hasBumpMap(tex_index)) {
                bump_textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_BUMP];
            }
        }

        this.render_lists = (List<ModelState<?>>[]) new ArrayList<?>[numSprites];
        this.respond_render_lists = (List<ModelState<?>>[]) new ArrayList[numSprites];
        for (int i = 0; i < numSprites; i++) {
            render_lists[i] = new ArrayList<>();
            respond_render_lists[i] = new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    SpriteRenderer(@NonNull SpriteList sprite_list, @NonNull Texture texture,
            @NonNull InstancedSpriteRenderer instancedSpriteRenderer) {
        this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;

        int numSprites = sprite_list.getNumSprites();
        this.textures = new Texture[numSprites];
        Arrays.fill(textures, texture);
        this.team_textures = new Texture[numSprites];
        this.bump_textures = new Texture[numSprites];

        this.render_lists = (List<ModelState<?>>[]) new ArrayList<?>[numSprites];
        this.respond_render_lists = (List<ModelState<?>>[]) new ArrayList[numSprites];
        for (int i = 0; i < numSprites; i++) {
            render_lists[i] = new ArrayList<>();
            respond_render_lists[i] = new ArrayList<>();
        }
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
            respond_render_lists[index].add(model);
        } else {
            render_lists[index].add(model);
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

    public void getAllPicks(@NonNull Consumer<@NonNull Target> picks) {
        for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            pickFromList(render_lists[i], picks);
            render_lists[i].clear();
            pickFromList(respond_render_lists[i], picks);
            respond_render_lists[i].clear();
        }
        for (ModelState<?> model : no_detail_render_list) {
            picks.accept((Target) model.getModel());
        }
        clearRenderLists();
    }

    private void pickFromList(@NonNull List<@Nullable ModelState<?>> render_list, @NonNull Consumer<
            @NonNull Target> picks) {
        for (int i = 0; i < render_list.size(); i++) {
            ModelState<?> model = render_list.get(i);
            render_list.set(i, null);
            if (model.getModel() instanceof Target target) {
                picks.accept(target);
            }
        }
    }

    void renderAll() {
        for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            renderAll(i, textures[i], team_textures[i], bump_textures[i]);
        }
    }

    private void renderAll(int index, @NonNull Texture texture, @Nullable Texture teamTexture,
            @Nullable Texture bumpTexture) {
        List<ModelState<?>> render_list = render_lists[index];
        boolean modulate = sprite_list.getSprite(index).modulateColor();

        for (ModelState<?> modelState : render_list) {
            if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                RenderTools.draw(modelState.getModel());
            }
            // Standard sprites: If modulate, use Blend. If opaque/alpha, use A2C (Blend=False).
            // Depth Write = !modulate (Opaque writes depth, Effects don't).
            instancedSpriteRenderer.add(sprite_list, index, modelState.getAnimation(),
                    modelState.getAnimationTicks(), texture, teamTexture, bumpTexture, false, modulate,
                    !modulate, true, modelState.getTransform(tempMatrix), modelState.getColor(), modelState
                            .getTeamColor());
        }
        render_list.clear();

        render_list = respond_render_lists[index];
        if (!render_list.isEmpty()) {
            for (ModelState<?> model : render_list) {
                if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                instancedSpriteRenderer.add(sprite_list, index, model.getAnimation(),
                        model.getAnimationTicks(), texture, teamTexture, bumpTexture, true, true, false,
                        true, model.getTransform(tempMatrix), Color.Linear.WHITE, Color.Linear.WHITE);
            }
            render_list.clear();
        }
    }

    void renderNoDetail() {
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
                // Disable depth test for no-detail sprites (overlays). Enable blend. No Depth Write.
                instancedSpriteRenderer.add(quadList, 0, 0, 0f, instancedSpriteRenderer.getWhiteTexture(), null,
                        null, false, true, false, false, tempMatrix, model.getTeamColor(), Color.Linear.TRANSPARENT);
            }
        }
        clearRenderLists();
    }

    void addInstance(int spriteIndex, int animation, float animTicks,
            boolean respond, boolean blend, boolean depthWrite, boolean depthTest,
            @NonNull Matrix4f modelMatrix, @NonNull Color color, @NonNull Color decalColor) {
        instancedSpriteRenderer.add(sprite_list, spriteIndex, animation, animTicks,
                textures[spriteIndex], team_textures[spriteIndex], bump_textures[spriteIndex],
                respond, blend, depthWrite, depthTest, modelMatrix, color, decalColor);
    }
}
