package com.oddlabs.tt.render;

import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RenderQueues {
	private final List<SpriteRenderer> sprite_renderers = new ArrayList<>();
	private final List<SpriteRenderer> blend_sprite_renderers = new ArrayList<>();

	private final List<SpriteRenderer> sprite_list_lookup = new ArrayList<>();
	private final List<ShadowListRenderer> shadow_renderer_lookup = new ArrayList<>();
	private final Map<Supplier<?>, ShadowListKey> desc_to_shadow_key = new HashMap<>();
	private final List<Texture> texture_lookup = new ArrayList<>();

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

	Texture getTexture(@NonNull TextureKey key) {
		return texture_lookup.get(key.getKey());
	}

	public @NonNull ShadowListKey registerRespondRenderer(Supplier<Texture[]> desc) {
		ShadowListKey key = desc_to_shadow_key.get(desc);
		if (key != null)
			return key;
		ShadowListRenderer renderer = new TargetRespondRenderer(desc);
		return register(desc, renderer);
	}

	private @NonNull ShadowListKey register(Supplier<?> desc, ShadowListRenderer renderer) {
		int index = shadow_renderer_lookup.size();
		shadow_renderer_lookup.add(renderer);
		ShadowListKey key = new ShadowListKey(index);
		desc_to_shadow_key.put(desc, key);
		return key;
	}

	public @NonNull ShadowListKey registerSelectableShadowList(Supplier<Texture[]> desc) {
		ShadowListKey key = desc_to_shadow_key.get(desc);
		if (key != null)
			return key;
		ShadowListRenderer renderer = new SelectableShadowRenderer(desc);
		return register(desc, renderer);
	}

	ShadowListRenderer getShadowRenderer(@NonNull ShadowListKey key) {
		return shadow_renderer_lookup.get(key.getKey());
	}

	public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file) {
		return register(sprite_file, 0);
	}

	public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file, int tex_index) {
		int index = sprite_list_lookup.size();
		SpriteList sprite_list = Resources.findResource(sprite_file);
		SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, tex_index);
		sprite_list_lookup.add(sprite_renderer);
		registerSpriteRenderer(sprite_renderer);
		return new SpriteKey(index, sprite_list.getBounds(), sprite_list.getAnimationTypes());
	}

	public SpriteRenderer getRenderer(@NonNull SpriteKey key) {
		return sprite_list_lookup.get(key.getKey());
	}

	private void registerSpriteRenderer(@NonNull SpriteRenderer sprite_renderer) {
		if (sprite_renderer.getSpriteList().getSprite(0).modulateColor()) {
			blend_sprite_renderers.add(sprite_renderer);
		} else {
			sprite_renderers.add(sprite_renderer);
		}
	}

	void getAllPicks(@NonNull List<Target> pick_list) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
	}

	void renderAll() {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.renderAll();
        }
	}

	void renderBlends() {
        for (SpriteRenderer blendSpriteRenderer : blend_sprite_renderers) {
            blendSpriteRenderer.renderAll();
        }
	}

	void renderShadows(LandscapeRenderer renderer) {
        for (ShadowListRenderer shadowListRenderer : shadow_renderer_lookup) {
            shadowListRenderer.renderShadows(renderer);
        }
	}
}
