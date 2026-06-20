package com.oddlabs.tt.model;

import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

/**
 * Template defining the static properties, costs, and visual representations of a building type.
 */
public final class BuildingTemplate extends Template {
    private final @NonNull BuildingType building_type;
    private final int placing_size;
    private final float smoke_radius;
    private final float smoke_height;
    private final int num_fragments;
    private final @NonNull BoundingBox @NonNull [] built_bounds;
    private final @NonNull BoundingBox @NonNull [] halfbuilt_bounds;
    private final @NonNull BoundingBox @NonNull [] start_bounds;
    private final int max_hit_points;
    private final UnitContainerFactory unit_container_factory;
    private final float mount_offset;
    private final float built_selection_radius;
    private final float built_selection_height;
    private final float halfbuilt_selection_radius;
    private final float halfbuilt_selection_height;
    private final float start_selection_radius;
    private final float start_selection_height;
    private final @NonNull Vector3fc rally;
    private final @NonNull Vector3fc chimney;

    public BuildingTemplate(
            @NonNull BuildingType building_type,
            int placing_size,
            float smoke_radius,
            float smoke_height,
            int num_fragments,
            float shadow_diameter,
            @NonNull BoundingBox @NonNull [] built_bounds, float built_selection_radius, float built_selection_height,
            @NonNull BoundingBox @NonNull [] halfbuilt_bounds, float halfbuilt_selection_radius,
            float halfbuilt_selection_height,
            @NonNull BoundingBox @NonNull [] start_bounds, float start_selection_radius, float start_selection_height,
            int max_hit_points,
            UnitContainerFactory unit_container_factory,
            @NonNull Abilities abilities,
            float @NonNull [] hit_offset_z,
            float mount_offset,
            float no_detail_size,
            float defense_chance,
            @NonNull Vector3fc rally,
            @NonNull Vector3fc chimney,
            @NonNull String name) {
        super(abilities, shadow_diameter, hit_offset_z, no_detail_size, defense_chance, name);
        this.building_type = building_type;
        this.built_selection_radius = built_selection_radius;
        this.built_selection_height = built_selection_height;
        this.halfbuilt_selection_radius = halfbuilt_selection_radius;
        this.halfbuilt_selection_height = halfbuilt_selection_height;
        this.start_selection_radius = start_selection_radius;
        this.start_selection_height = start_selection_height;
        this.placing_size = placing_size;
        this.smoke_radius = smoke_radius;
        this.smoke_height = smoke_height;
        this.num_fragments = num_fragments;
        this.built_bounds = built_bounds;
        this.halfbuilt_bounds = halfbuilt_bounds;
        this.start_bounds = start_bounds;

        this.max_hit_points = max_hit_points;
        this.unit_container_factory = unit_container_factory;
        this.mount_offset = mount_offset;
        this.rally = rally;
        this.chimney = chimney;
    }

    public @NonNull BuildingType getBuildingType() {
        return building_type;
    }

    public float getBuiltSelectionRadius() {
        return built_selection_radius;
    }

    public float getBuiltSelectionHeight() {
        return built_selection_height;
    }

    public float getHalfbuiltSelectionRadius() {
        return halfbuilt_selection_radius;
    }

    public float getHalfbuiltSelectionHeight() {
        return halfbuilt_selection_height;
    }

    public float getStartSelectionRadius() {
        return start_selection_radius;
    }

    public float getStartSelectionHeight() {
        return start_selection_height;
    }

    public int getPlacingSize() {
        return placing_size;
    }

    public float getSmokeRadius() {
        return smoke_radius;
    }

    public float getSmokeHeight() {
        return smoke_height;
    }

    public int getNumFragments() {
        return num_fragments;
    }

    public @NonNull BoundingBox @NonNull [] getBuiltBounds() {
        return built_bounds;
    }

    public @NonNull BoundingBox @NonNull [] getStartBounds() {
        return start_bounds;
    }

    public @NonNull BoundingBox @NonNull [] getHalfbuiltBounds() {
        return halfbuilt_bounds;
    }


    public int getMaxHitPoints() {
        return max_hit_points;
    }

    public UnitContainerFactory getUnitContainerFactory() {
        return unit_container_factory;
    }

    public float getMountOffset() {
        return mount_offset;
    }

    public @NonNull Vector3fc getRally() {
        return rally;
    }

    public @NonNull Vector3fc getChimney() {
        return chimney;
    }
}
