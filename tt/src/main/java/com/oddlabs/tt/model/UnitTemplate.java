package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.render.ShadowListKey;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;

public final class UnitTemplate extends Template {
    private final float meters_per_second;
    private final @NonNull WeaponFactory weapon_factory;
    private final @NonNull SpriteKey sprite_renderer;
    private final @NonNull Map<String, SpriteKey> attachments;
    private final @NonNull Set<String> default_attachments;
    private final UnitSupplyContainerFactory supply_container_factory;
    private final @NonNull Audio death_sound;
    private final float death_pitch;
    private final float selection_radius;
    private final float selection_height;
    private final int max_hit_points;
    private final float stun_x;
    private final float stun_y;
    private final float stun_z;
    private final int status_value;

    public UnitTemplate(float selection_radius,
            float selection_height,
            @NonNull Abilities abilities,
            float meters_per_second,
            @NonNull WeaponFactory weapon_factory,
            @NonNull SpriteKey sprite_renderer,
            float shadow_diameter,
            @NonNull ShadowListKey shadow_renderer,
            UnitSupplyContainerFactory supply_container_factory,
            @NonNull Audio death_sound,
            float death_pitch,
            float @NonNull [] hit_offset_z,
            float no_detail_size,
            float defense_chance,
            @NonNull String name,
            int max_hit_points,
            float stun_x,
            float stun_y,
            float stun_z,
            int status_value) {
        this(selection_radius, selection_height, abilities, meters_per_second, weapon_factory, sprite_renderer, shadow_diameter, shadow_renderer, supply_container_factory, death_sound, death_pitch, hit_offset_z, no_detail_size, defense_chance, name, max_hit_points, stun_x, stun_y, stun_z, status_value, Map.of());
    }

    public UnitTemplate(float selection_radius,
            float selection_height,
            @NonNull Abilities abilities,
            float meters_per_second,
            @NonNull WeaponFactory weapon_factory,
            @NonNull SpriteKey sprite_renderer,
            float shadow_diameter,
            @NonNull ShadowListKey shadow_renderer,
            UnitSupplyContainerFactory supply_container_factory,
            @NonNull Audio death_sound,
            float death_pitch,
            float @NonNull [] hit_offset_z,
            float no_detail_size,
            float defense_chance,
            @NonNull String name,
            int max_hit_points,
            float stun_x,
            float stun_y,
            float stun_z,
            int status_value,
            @NonNull Map<String, SpriteKey> attachments) {
        this(selection_radius, selection_height, abilities, meters_per_second, weapon_factory, sprite_renderer, shadow_diameter, shadow_renderer, supply_container_factory, death_sound, death_pitch, hit_offset_z, no_detail_size, defense_chance, name, max_hit_points, stun_x, stun_y, stun_z, status_value, attachments, Set.of());
    }

    public UnitTemplate(float selection_radius,
            float selection_height,
            @NonNull Abilities abilities,
            float meters_per_second,
            @NonNull WeaponFactory weapon_factory,
            @NonNull SpriteKey sprite_renderer,
            float shadow_diameter,
            @NonNull ShadowListKey shadow_renderer,
            UnitSupplyContainerFactory supply_container_factory,
            @NonNull Audio death_sound,
            float death_pitch,
            float @NonNull [] hit_offset_z,
            float no_detail_size,
            float defense_chance,
            @NonNull String name,
            int max_hit_points,
            float stun_x,
            float stun_y,
            float stun_z,
            int status_value,
            @NonNull Map<String, SpriteKey> attachments,
            @NonNull Set<String> default_attachments) {
        super(abilities, shadow_diameter, shadow_renderer, hit_offset_z, no_detail_size, defense_chance, name);
        this.attachments = attachments;
        this.default_attachments = default_attachments;
        this.selection_radius = selection_radius;
        this.selection_height = selection_height;
        this.meters_per_second = meters_per_second;
        this.weapon_factory = weapon_factory;
        this.sprite_renderer = sprite_renderer;
        this.supply_container_factory = supply_container_factory;
        this.death_sound = death_sound;
        this.death_pitch = death_pitch;
        this.max_hit_points = max_hit_points;
        this.stun_x = stun_x;
        this.stun_y = stun_y;
        this.stun_z = stun_z;
        this.status_value = status_value;
    }

    public float getSelectionRadius() {
        return selection_radius;
    }

    public float getSelectionHeight() {
        return selection_height;
    }

    public float getMetersPerSecond() {
        return meters_per_second;
    }

    public @NonNull WeaponFactory getWeaponFactory() {
        return weapon_factory;
    }

    public @NonNull SpriteKey getSpriteRenderer() {
        return sprite_renderer;
    }

    /**
     * Optional render-only sprites that share this unit's skeleton and clips, keyed by attachment name.
     */
    public @NonNull Map<String, SpriteKey> getAttachments() {
        return attachments;
    }

    /**
     * Names from {@link #getAttachments()} that every new unit starts with (a held weapon, say).
     */
    public @NonNull Set<String> getDefaultAttachments() {
        return default_attachments;
    }

    public UnitSupplyContainerFactory getUnitSupplyContainerFactory() {
        return supply_container_factory;
    }

    public @NonNull Audio getDeathSound() {
        return death_sound;
    }

    public float getDeathPitch() {
        return death_pitch;
    }

    public int getMaxHitPoints() {
        return max_hit_points;
    }

    public float getStunX() {
        return stun_x;
    }

    public float getStunY() {
        return stun_y;
    }

    public float getStunZ() {
        return stun_z;
    }

    public int getStatusValue() {
        return status_value;
    }
}
