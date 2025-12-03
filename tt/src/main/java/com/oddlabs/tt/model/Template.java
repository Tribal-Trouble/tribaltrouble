package com.oddlabs.tt.model;

import com.oddlabs.tt.render.ShadowListKey;
import org.jspecify.annotations.NonNull;

public abstract class Template {
	private final @NonNull Abilities abilities;
	private final @NonNull ShadowListKey shadow_renderer;
	private final float[] hit_offset_z;
	private final float no_detail_size;
	private final float defense_chance;
	private final float shadow_diameter;
	private final @NonNull String name;

	protected Template(@NonNull Abilities abilities, float shadow_diameter, @NonNull ShadowListKey shadow_renderer, float[] hit_offset_z, float no_detail_size, float defense_chance, @NonNull String name) {
		this.abilities = abilities;
		this.shadow_renderer = shadow_renderer;
		this.hit_offset_z = hit_offset_z;
		this.no_detail_size = no_detail_size;
		this.defense_chance = defense_chance;
		this.name = name;
		this.shadow_diameter = shadow_diameter;
	}

	public final @NonNull String getName() {
		return name;
	}

	public final @NonNull Abilities getAbilities() {
		return abilities;
	}

	public final float getShadowDiameter() {
		return shadow_diameter;
	}

	public final @NonNull ShadowListKey getSelectableShadowRenderer() {
		return shadow_renderer;
	}

	public final float getHitOffsetZ(int index) {
		return hit_offset_z[index];
	}

	public final float getNoDetailSize() {
		return no_detail_size;
	}

	public final float getDefenseChance() {
		return defense_chance;
	}
}
