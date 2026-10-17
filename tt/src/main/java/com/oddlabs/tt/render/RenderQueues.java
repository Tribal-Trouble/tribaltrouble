package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.GeometryRegistry;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RenderQueues implements AutoCloseable {
    private static final int FIRST_TEXTURE = 0;

    private final List<@NonNull SpriteRenderer> sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> blend_sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> plant_renderers = new ArrayList<>();

    private final List<@NonNull SpriteRenderer> sprite_list_lookup = new ArrayList<>();
    private final List<@NonNull SpriteFile> sprite_file_lookup = new ArrayList<>();
    private final List<@NonNull Integer> tex_index_lookup = new ArrayList<>();
    private final List<@NonNull List<@NonNull SpriteKey>> props_lookup = new ArrayList<>();
    private final @NonNull GlowLights glow_lights = new GlowLights();
    // Shadow renderers own shaders and buffers, so they are described at registration and created on first use.
    private final List<@NonNull Supplier<@NonNull ShadowListRenderer>> shadow_renderer_lookup = new ArrayList<>();
    private final List<@Nullable ShadowListRenderer> shadow_renderer_cache = new ArrayList<>();
    private final Map<@NonNull Supplier<@NonNull Texture @NonNull []>, @NonNull ShadowListKey> desc_to_shadow_key = new HashMap<>();
    // Textures are described at registration and created on first lookup, so resources can be registered
    // without a GL context.
    private final List<@NonNull Supplier<@NonNull Texture>> texture_lookup = new ArrayList<>();
    private final List<@Nullable Texture> texture_cache = new ArrayList<>();
    private final InstancedSpriteRenderer spriteRenderer = new InstancedSpriteRenderer();

    public RenderQueues() {
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture[]> desc, int index) {
        TextureKey key = new TextureKey(texture_lookup.size());
        texture_lookup.add(() -> Resources.findResource(desc)[index]);
        texture_cache.add(null);
        return key;
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture> desc) {
        TextureKey key = new TextureKey(texture_lookup.size());
        texture_lookup.add(() -> Resources.findResource(desc));
        texture_cache.add(null);
        return key;
    }

    @NonNull
    Texture getTexture(@NonNull TextureKey key) {
        Texture texture = texture_cache.get(key.getKey());
        if (texture == null) {
            texture = texture_lookup.get(key.getKey()).get();
            texture_cache.set(key.getKey(), texture);
        }
        return texture;
    }

    public @NonNull ShadowListKey registerRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        if (key != null)
            return key;
        return register(desc, () -> new TargetRespondRenderer(desc));
    }

    private @NonNull ShadowListKey register(@NonNull Supplier<@NonNull Texture @NonNull []> desc,
            @NonNull Supplier<@NonNull ShadowListRenderer> renderer) {
        int index = shadow_renderer_lookup.size();
        shadow_renderer_lookup.add(renderer);
        shadow_renderer_cache.add(null);
        ShadowListKey key = new ShadowListKey(index);
        desc_to_shadow_key.put(desc, key);
        return key;
    }

    public @NonNull ShadowListKey registerSelectableShadowList(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        return key != null ? key : register(desc, () -> new SelectableShadowRenderer(desc));
    }

    private @NonNull ShadowListRenderer shadowRenderer(int index) {
        ShadowListRenderer renderer = shadow_renderer_cache.get(index);
        if (renderer == null) {
            renderer = shadow_renderer_lookup.get(index).get();
            shadow_renderer_cache.set(index, renderer);
        }
        return renderer;
    }

    @NonNull
    ShadowListRenderer getShadowRenderer(@NonNull ShadowListKey key) {
        return shadowRenderer(key.getKey());
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file) {
        return register(sprite_file, FIRST_TEXTURE);
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file, int tex_index) {
        int index = sprite_list_lookup.size();
        SpriteList sprite_list = Resources.findResource(sprite_file);
        // The key keeps the stock bounds and clips, so an event skin changes only what is drawn.
        SpriteFile drawn_file = GeometryRegistry.eventSkin(sprite_file);
        SpriteList drawn = Resources.findResource(drawn_file);
        if (drawn.getAnimationTypes().length != sprite_list.getAnimationTypes().length)
            throw new IllegalStateException(
                    "Event skin of " + sprite_file.getLocation() + " has other clips than it; give it that sprite as its base");
        int drawn_index = tex_index < drawn.getSprite(0).getNumTextures() ? tex_index : FIRST_TEXTURE;
        SpriteRenderer sprite_renderer = new SpriteRenderer(drawn, drawn_index, spriteRenderer,
                GeometryRegistry.getLights(drawn_file.getLocation(), drawn_index), glow_lights);
        sprite_list_lookup.add(sprite_renderer);
        sprite_file_lookup.add(sprite_file);
        tex_index_lookup.add(tex_index);
        props_lookup.add(List.of());
        registerSpriteRenderer(sprite_renderer, sprite_file.getLocation());
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        int[] type_array = new int[animation_types.length];
        for (int i = 0; i < animation_types.length; i++) {
            type_array[i] = animation_types[i].ordinal();
        }
        // A skin only changes the look: the sprite keeps its own props and the skin adds its own.
        List<String> props = new ArrayList<>(GeometryRegistry.getProps(sprite_file.getLocation()));
        if (!drawn_file.getLocation().equals(sprite_file.getLocation()))
            props.addAll(GeometryRegistry.getProps(drawn_file.getLocation()));
        props_lookup.set(index, props.stream().map(prop -> register(sprite_file.withLocation(prop),
                tex_index)).toList());
        return new SpriteKey(index, sprite_list.getBounds(), type_array);
    }

    public @NonNull GlowLights getGlowLights() {
        return glow_lights;
    }

    /** Extra meshes drawn at the sprite's transform, never part of its bounds or clips. */
    public @NonNull List<@NonNull SpriteKey> getProps(@NonNull SpriteKey key) {
        return props_lookup.get(key.getKey());
    }

    public @NonNull SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return sprite_list_lookup.get(key.getKey());
    }

    /** The texture the sprite was registered with, before any event skin with fewer textures fell back to its first. */
    public int getTexIndex(@NonNull SpriteKey key) {
        return tex_index_lookup.get(key.getKey());
    }

    public @NonNull SpriteFile getSpriteFile(@NonNull SpriteKey key) {
        return sprite_file_lookup.get(key.getKey());
    }

    public @NonNull InstancedSpriteRenderer getInstancedRenderer() {
        return spriteRenderer;
    }

    private void registerSpriteRenderer(@NonNull SpriteRenderer sprite_renderer, @NonNull String location) {
        if (sprite_renderer.getSpriteList().getSprite(0).modulateColor()) {
            blend_sprite_renderers.add(sprite_renderer);
        } else if (location.contains("plant") || location.contains("leaf")) {
            plant_renderers.add(sprite_renderer);
        } else {
            sprite_renderers.add(sprite_renderer);
        }
    }

    void getAllPicks(@NonNull List<@NonNull Target> pick_list) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
        for (SpriteRenderer spriteRenderer : plant_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
    }

    void renderAll(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderPlants(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        for (SpriteRenderer spriteRenderer : plant_renderers) {
            spriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderEmitterSprites(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        // Sprite-based particles (building debris) are deferred to SpriteListRenderer during
        // the emitter pass, which runs after the main renderAll(). Flush them here.
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderBlends(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        for (SpriteRenderer blendSpriteRenderer : blend_sprite_renderers) {
            blendSpriteRenderer.renderAll();
        }
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderNoDetail() {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderNoDetail();
        }
        for (SpriteRenderer spriteRenderer : plant_renderers) {
            spriteRenderer.renderNoDetail();
        }
        for (SpriteRenderer blendSpriteRenderer : blend_sprite_renderers) {
            blendSpriteRenderer.renderNoDetail();
        }
    }

    void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        for (int i = 0; i < shadow_renderer_lookup.size(); i++) {
            shadowRenderer(i).renderShadows(context, renderer, modelViewStack, projectionStack);
        }
    }

    @Override
    public void close() {
        spriteRenderer.close();
        for (SpriteList spriteList : sprite_list_lookup.stream().map(
                SpriteRenderer::getSpriteList).distinct().toList()) {
            spriteList.close();
        }
        for (ShadowListRenderer shadowListRenderer : shadow_renderer_cache) {
            if (shadowListRenderer != null) {
                shadowListRenderer.close();
            }
        }
    }
}
