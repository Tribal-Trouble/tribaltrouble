package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.ShadowListKey;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class BuildingTemplate extends Template {
    public static final int TYPE_BUILDING = 0;
    public static final int TYPE_SHIP = 1;

    private final int template_id;
    private final int type;
    private final int placing_size;
    private final float smoke_radius;
    private final float smoke_height;
    private final int num_fragments;
    private final @NonNull SpriteKey built_renderer;
    private final @NonNull SpriteKey halfbuilt_renderer;
    private final @NonNull SpriteKey start_renderer;
    private final int max_hit_points;
    private final UnitContainerFactory unit_container_factory;
    private final float mount_offset;
    private final float built_selection_radius;
    private final float built_selection_height;
    private final float halfbuilt_selection_radius;
    private final float halfbuilt_selection_height;
    private final float start_selection_radius;
    private final float start_selection_height;
    private final float rally_x;
    private final float rally_y;
    private final float rally_z;
    private final float chimney_x;
    private final float chimney_y;
    private final float chimney_z;
    private final boolean near_sea;

    public BuildingTemplate(
            int template_id,
            int type,
            int placing_size,
            float smoke_radius,
            float smoke_height,
            int num_fragments,
            float shadow_diameter,
            @NonNull ShadowListKey shadow_renderer,
            @NonNull SpriteKey built_renderer, float built_selection_radius, float built_selection_height,
            @NonNull SpriteKey halfbuilt_renderer, float halfbuilt_selection_radius, float halfbuilt_selection_height,
            @NonNull SpriteKey start_renderer, float start_selection_radius, float start_selection_height,
            int max_hit_points,
            UnitContainerFactory unit_container_factory,
            @NonNull Abilities abilities,
            float @NonNull [] hit_offset_z,
            float mount_offset,
            float no_detail_size,
            float defense_chance,
            float rally_x,
            float rally_y,
            float rally_z,
            float chimney_x,
            float chimney_y,
            float chimney_z,
            boolean near_sea,
            @NonNull String name) {
        super(abilities, shadow_diameter, shadow_renderer, hit_offset_z, no_detail_size, defense_chance, name);
        this.template_id = template_id;
        this.type = type;
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
        this.built_renderer = built_renderer;
        this.halfbuilt_renderer = halfbuilt_renderer;
        this.start_renderer = start_renderer;
        this.max_hit_points = max_hit_points;
        this.unit_container_factory = unit_container_factory;
        this.mount_offset = mount_offset;
        this.rally_x = rally_x;
        this.rally_y = rally_y;
        this.rally_z = rally_z;
        this.chimney_x = chimney_x;
        this.chimney_y = chimney_y;
        this.chimney_z = chimney_z;
        this.near_sea = near_sea;
    }

    public int getTemplateID() {
        return template_id;
    }

    public final int getType() {
        return type;
    }

    public final Building create(Player owner, int grid_x, int grid_y) {
        if (type == TYPE_SHIP) {
            return new Ship(owner, this, grid_x, grid_y);
        }
        return new LandBuilding(owner, this, grid_x, grid_y);
    }

    public final boolean isPlacingLegal(UnitGrid unit_grid, int grid_x, int grid_y) {
        if (type == TYPE_SHIP) {
            return Ship.isPlacingLegal(unit_grid, this, grid_x, grid_y);
        }
        return LandBuilding.isPlacingLegal(unit_grid, this, grid_x, grid_y);
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

    public @NonNull SpriteKey getBuiltRenderer() {
        return built_renderer;
    }

    public @NonNull SpriteKey getStartRenderer() {
        return start_renderer;
    }

    public @NonNull SpriteKey getHalfbuiltRenderer() {
        return halfbuilt_renderer;
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

    public float getRallyX() {
        return rally_x;
    }

    public float getRallyY() {
        return rally_y;
    }

    public float getRallyZ() {
        return rally_z;
    }

    public float getChimneyX() {
        return chimney_x;
    }

    public float getChimneyY() {
        return chimney_y;
    }

    public float getChimneyZ() {
        return chimney_z;
    }

    public final boolean isNearSea() {
        return near_sea;
    }
}
