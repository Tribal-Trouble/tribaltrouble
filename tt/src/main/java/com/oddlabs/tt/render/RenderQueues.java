package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.model.Target;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the collection of renderers and texture/sprite registries.
 * Acts as a central hub for accessing various specialized rendering systems.
 */
public final class RenderQueues implements AutoCloseable {
    private final List<@NonNull SpriteRenderer> sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> blend_sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> plant_renderers = new ArrayList<>();

    private final List<@NonNull SpriteRenderer> sprite_list_lookup = new ArrayList<>();
    private final List<@NonNull ShadowListRenderer> shadow_renderer_lookup = new ArrayList<>();
    private final Map<@NonNull Supplier<@NonNull Texture @NonNull []>, @NonNull ShadowListKey> desc_to_shadow_key
            = new HashMap<>();
    private final List<@NonNull Texture> texture_lookup = new ArrayList<>();
    /** Shared Array for particle effects */
    private @Nullable TextureArray effect_texture_array;
    private final Map<@NonNull Integer, @NonNull GLImage @NonNull []> pending_array_uploads = new HashMap<>();

    private final InstancedSpriteRenderer spriteRenderer = new InstancedSpriteRenderer();
    private final DecalRenderer decalRenderer = new DecalRenderer();
    private final EmitterRenderer emitterRenderer = new EmitterRenderer();

    public RenderQueues() {
    }

    @NonNull
    TextureArray getEffectTextureArray() {
        return ensureTextureArray();
    }

    public @NonNull TextureKey registerEffectTexture(@NonNull Supplier<Texture[]> desc, int index, int layer) {
        assert effect_texture_array == null : "Cannot register effect textures after the array has been built";
        TextureKey key = registerTexture(desc, index);
        Texture texture = getTexture(key);
        texture.setLayer(layer);

        GLImage[] mipmaps = texture.getSourceMipmaps();
        if (mipmaps != null) {
            pending_array_uploads.put(layer, mipmaps);
            texture.setSourceMipmaps(null);
        } else if (texture.getSourceDXT() != null) {
            throw new IllegalArgumentException(
                    "Compressed DXT textures are not supported as source for the effect array.");
        }
        return key;
    }

    public @NonNull TextureKey registerEffectTexture(@NonNull Supplier<Texture> desc, int layer) {
        assert effect_texture_array == null : "Cannot register effect textures after the array has been built";
        TextureKey key = registerTexture(desc);
        Texture texture = getTexture(key);
        texture.setLayer(layer);

        GLImage[] mipmaps = texture.getSourceMipmaps();
        if (mipmaps != null) {
            pending_array_uploads.put(layer, mipmaps);
            texture.setSourceMipmaps(null);
        } else if (texture.getSourceDXT() != null) {
            throw new IllegalArgumentException(
                    "Compressed DXT textures are not supported as source for the effect array.");
        }
        return key;
    }

    public @NonNull TextureArray ensureTextureArray() {
        if (effect_texture_array != null) {
            assert pending_array_uploads.isEmpty() : "Pending effect textures found but array is already built";
            return effect_texture_array;
        }

        assert !pending_array_uploads.isEmpty() : "No effects textures registered";

        // Determine optimal array size
        int slotWidth = 0;
        int slotHeight = 0;
        for (GLImage[] mipmaps : pending_array_uploads.values()) {
            slotWidth = Math.max(slotWidth, mipmaps[0].getWidth());
            slotHeight = Math.max(slotHeight, mipmaps[0].getHeight());
        }
        slotWidth = Utils.nextPowerOf2(slotWidth);
        slotHeight = Utils.nextPowerOf2(slotHeight);

        int maxLayer = -1;
        for (int layer : pending_array_uploads.keySet()) {
            maxLayer = Math.max(maxLayer, layer);
        }
        int depth = maxLayer + 1;
        assert pending_array_uploads.size() == depth : "Gaps found in effect texture layers";

        effect_texture_array = new TextureArray(slotWidth, slotHeight, depth,
                Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);

        // Pre-scale all sources to match array dimensions
        for (var entry : pending_array_uploads.entrySet()) {
            GLImage[] mipmaps = entry.getValue();
            if (mipmaps[0].getWidth() != slotWidth || mipmaps[0].getHeight() != slotHeight) {
                GLImage scaled = mipmaps[0].scale(slotWidth, slotHeight);
                entry.setValue(scaled.createMipMaps());
            }
        }

        effect_texture_array.build(pending_array_uploads, Globals.COMPRESSED_RGBA_FORMAT);
        pending_array_uploads.clear();

        return effect_texture_array;
    }

    @NonNull
    EmitterRenderer getEmitterRenderer() {
        return emitterRenderer;
    }

    @NonNull
    DecalRenderer getDecalRenderer() {
        return decalRenderer;
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

    @NonNull
    Texture getTexture(@NonNull TextureKey key) {
        return texture_lookup.get(key.key());
    }

    public @NonNull ShadowListKey registerRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        if (key != null)
            return key;
        ShadowListRenderer renderer = new TargetRespondRenderer(desc);
        return register(desc, renderer);
    }

    private @NonNull ShadowListKey register(@NonNull Supplier<@NonNull Texture @NonNull []> desc,
            @NonNull ShadowListRenderer renderer) {
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

    public @NonNull ShadowListKey registerCrackDecalList(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        return key != null ? key : register(desc, new CrackDecalRenderer(desc));
    }

    public @NonNull ShadowRenderer getDefaultShadowRenderer() {
        return getShadowRenderer(registerSelectableShadowList(VisualRegistry.DEFAULT_SHADOW_DESC));
    }

    @NonNull
    ShadowListRenderer getShadowRenderer(@NonNull ShadowListKey key) {
        return shadow_renderer_lookup.get(key.key());
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file) {
        return register(sprite_file, 0);
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file, int tex_index) {
        int index = sprite_list_lookup.size();
        SpriteList sprite_list = Resources.findResource(sprite_file);

        SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, tex_index, spriteRenderer);
        sprite_list_lookup.add(sprite_renderer);
        registerSpriteRenderer(sprite_renderer, sprite_file.getLocation());
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        return new SpriteKey(index, sprite_list.getBounds(), animation_types);
    }

    public @NonNull SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return sprite_list_lookup.get(key.key());
    }

    public @NonNull SpriteKey registerDynamicSprite(@NonNull SpriteList sprite_list, @NonNull Texture texture) {
        int index = sprite_list_lookup.size();

        SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, texture, spriteRenderer);
        sprite_list_lookup.add(sprite_renderer);
        registerSpriteRenderer(sprite_renderer, "dynamic_emoji");
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        return new SpriteKey(index, sprite_list.getBounds(), animation_types);
    }

    public @NonNull SpriteKey registerDynamicSprite(@NonNull SpriteList sprite_list, @NonNull TextureKey texture_key) {
        return registerDynamicSprite(sprite_list, getTexture(texture_key));
    }

    public @NonNull SpriteKey registerIconSprite(com.oddlabs.tt.gui.@NonNull IconQuad icon) {
        SpriteList sprite_list = SpriteList.createQuadInstance(icon.getU1(), icon.getV1(), icon.getU2(), icon.getV2());
        return registerDynamicSprite(sprite_list, icon.getTexture());
    }

    @NonNull
    InstancedSpriteRenderer getInstancedRenderer() {
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

    void getAllPicks(@NonNull Consumer<@NonNull Target> pick_list) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
        for (SpriteRenderer spriteRenderer : plant_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
    }

    public void renderAll(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        sprite_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderPlants(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        plant_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderBlends(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        blend_sprite_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderNoDetail() {
        sprite_renderers.forEach(SpriteRenderer::renderNoDetail);
        plant_renderers.forEach(SpriteRenderer::renderNoDetail);
        blend_sprite_renderers.forEach(SpriteRenderer::renderNoDetail);
    }

    void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        decalRenderer.clear();
        try (var _ = decalRenderer.setup(context, renderer, modelViewStack, projectionStack)) {
            for (ShadowListRenderer shadowListRenderer : shadow_renderer_lookup) {
                shadowListRenderer.renderShadows(context, this, renderer, modelViewStack, projectionStack);
            }
        }
    }

    void renderParticles(@NonNull RenderContext context, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack, @NonNull Texture depthTexture) {
        assert pending_array_uploads.isEmpty() : "Attempting to render particles before effect array is built";
        emitterRenderer.render(context, this, state, modelViewStack, projectionStack, depthTexture);
    }

    @Override
    public void close() {
        spriteRenderer.close();
        decalRenderer.close();
        emitterRenderer.close();
        for (SpriteList spriteList : sprite_list_lookup.stream().map(SpriteRenderer::getSpriteList).distinct()
                .toList()) {
            spriteList.close();
        }
        shadow_renderer_lookup.forEach(ShadowListRenderer::close);
    }
}
