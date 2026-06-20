package com.oddlabs.tt.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A template defining the base characteristics and assets for a unit type.
 */
public final class UnitTemplate extends Template {
    private final float meters_per_second;
    private final @NonNull WeaponFactory weapon_factory;
    private final @NonNull UnitVisualType visual_type;
    private final @NonNull BoundingBox @NonNull [] bounds;
    private final AnimationInfo.@NonNull AnimationType @NonNull [] anim_types;
    private final @Nullable UnitSupplyContainerFactory supply_container_factory;
    private final @NonNull AudioFile death_sound;
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
            @NonNull UnitVisualType visual_type,
            @NonNull BoundingBox @NonNull [] bounds,
            AnimationInfo.@NonNull AnimationType @NonNull [] anim_types,
            float shadow_diameter,
            @Nullable UnitSupplyContainerFactory supply_container_factory,
            @NonNull AudioFile death_sound,
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
        super(abilities, shadow_diameter, hit_offset_z, no_detail_size, defense_chance, name);
        this.selection_radius = selection_radius;
        this.selection_height = selection_height;
        this.meters_per_second = meters_per_second;
        this.weapon_factory = weapon_factory;
        this.visual_type = visual_type;
        this.bounds = bounds;
        this.anim_types = anim_types;
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

    public @NonNull UnitVisualType getVisualType() {
        return visual_type;
    }

    public @NonNull BoundingBox @NonNull [] getBounds() {
        return bounds;
    }

    /** {@return the animation type for the specified unit animation} */
    public AnimationInfo.@NonNull AnimationType getAnimationType(Unit.@NonNull Animation animation) {
        return anim_types[animation.ordinal()];
    }

    public UnitSupplyContainerFactory getUnitSupplyContainerFactory() {
        return supply_container_factory;
    }

    public @NonNull AudioFile getDeathSound() {
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
