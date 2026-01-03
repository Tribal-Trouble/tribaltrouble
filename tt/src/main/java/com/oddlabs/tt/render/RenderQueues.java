package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RenderQueues implements AutoCloseable {
    private final List<@NonNull SpriteRenderer> sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> blend_sprite_renderers = new ArrayList<>();

    private final List<@NonNull SpriteRenderer> sprite_list_lookup = new ArrayList<>();
    private final List<@NonNull ShadowListRenderer> shadow_renderer_lookup = new ArrayList<>();
    private final Map<@NonNull Supplier<@NonNull Texture @NonNull []>, @NonNull ShadowListKey> desc_to_shadow_key = new HashMap<>();
    private final List<@NonNull Texture> texture_lookup = new ArrayList<>();
    private final InstancedSpriteRenderer spriteRenderer = new InstancedSpriteRenderer();

    public RenderQueues() {
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture[]> desc, int index) {
        TextureKey key = new TextureKey(texture_lookup.size());
        Texture[] textures = Resources.findResource(desc);
        texture_lookup.add(textures[index]);
        return key;
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture> desc) {
        TextureKey key = new TextureKey(texture_lookup.size());
        texture_lookup.add(Resources.findResource(desc));
        return key;
    }

    @NonNull Texture getTexture(@NonNull TextureKey key) {
        return texture_lookup.get(key.getKey());
    }

    public @NonNull ShadowListKey registerRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        if (key != null)
            return key;
        ShadowListRenderer renderer = new TargetRespondRenderer(desc);
        return register(desc, renderer);
    }

    private @NonNull ShadowListKey register(@NonNull Supplier<@NonNull Texture @NonNull []> desc, @NonNull ShadowListRenderer renderer) {
        int index = shadow_renderer_lookup.size();
        shadow_renderer_lookup.add(renderer);
        ShadowListKey key = new ShadowListKey(index);
        desc_to_shadow_key.put(desc, key);
        return key;
    }

    public @NonNull ShadowListKey registerSelectableShadowList(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        return key != null ? key : register(desc, new SelectableShadowRenderer(desc));
    }

    @NonNull ShadowListRenderer getShadowRenderer(@NonNull ShadowListKey key) {
        return shadow_renderer_lookup.get(key.getKey());
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file) {
        return register(sprite_file, 0);
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file, int tex_index) {
        int index = sprite_list_lookup.size();
        SpriteList sprite_list = Resources.findResource(sprite_file);
        SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, tex_index, spriteRenderer);
        sprite_list_lookup.add(sprite_renderer);
        registerSpriteRenderer(sprite_renderer);
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        int[] type_array = new int[animation_types.length];
        for (int i = 0; i < animation_types.length; i++) {
            type_array[i] = animation_types[i].ordinal();
        }
        return new SpriteKey(index, sprite_list.getBounds(), type_array);
    }

    public @NonNull SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return sprite_list_lookup.get(key.getKey());
    }

    public @NonNull InstancedSpriteRenderer getInstancedRenderer() {
        return spriteRenderer;
    }

    private void registerSpriteRenderer(@NonNull SpriteRenderer sprite_renderer) {
        if (sprite_renderer.getSpriteList().getSprite(0).modulateColor()) {
            blend_sprite_renderers.add(sprite_renderer);
        } else {
            sprite_renderers.add(sprite_renderer);
        }
    }

    void getAllPicks(@NonNull List<@NonNull Target> pick_list) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
    }

    void renderAll(@NonNull CameraState camera_state, @NonNull MatrixStack projectionStack) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(camera_state, projectionStack);
    }

    void renderBlends(@NonNull CameraState camera_state, @NonNull MatrixStack projectionStack) {
        for (SpriteRenderer blendSpriteRenderer : blend_sprite_renderers) {
            blendSpriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(camera_state, projectionStack);
    }

    void renderNoDetail() {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderNoDetail();
        }
        for (SpriteRenderer blendSpriteRenderer : blend_sprite_renderers) {
            blendSpriteRenderer.renderNoDetail();
        }
    }

    void renderShadows(@NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        for (ShadowListRenderer shadowListRenderer : shadow_renderer_lookup) {
            shadowListRenderer.renderShadows(renderer, modelViewStack, projectionStack);
        }
    }

    @Override
    public void close() {
        spriteRenderer.close();
    }
}
