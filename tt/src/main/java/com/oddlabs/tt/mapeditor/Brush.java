package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The brushes, in dropdown order: terrain shaping in one dropdown, then resource painting in the other, then the
 * spawn tool, which has a dropdown of players instead.
 */
enum Brush {
    HEIGHT("brush_height", "hint_height", null),
    FLATTEN("brush_flatten", "hint_flatten", null),
    SMOOTH("brush_smooth", "hint_smooth", null),
    ROUGHNESS("brush_roughness", "hint_roughness", null),
    RANDOM("brush_random", "hint_random", null),
    /** A straight ramp dragged from one end to the other. */
    RAMP("brush_ramp", "hint_ramp", null),
    /** A walkable path along a course clicked out a point at a time. */
    PATH("brush_path", "hint_path", null),
    ISTHMUS("brush_isthmus", "hint_isthmus", null),
    RIVER("brush_river", "hint_river", null),
    MOUNTAIN("brush_mountain", "hint_mountain", null),
    CLIFFS("brush_cliffs", "hint_cliffs", null),
    ERODE("brush_erode", "hint_erode", null),
    WARP("brush_warp", "hint_warp", null),
    /** Turns the ground under the brush about its middle as one piece. */
    TWIST("brush_twist", "hint_twist", null),
    /** Winds the ground under the brush into a spiral. */
    SWIRL("brush_swirl", "hint_swirl", null),
    /** Grabs the ground and drags it along like putty, skewing it up or down as it stretches. */
    STRETCH("brush_stretch", "hint_stretch", null),
    BEACH("brush_beach", "hint_beach", null),
    /** Copies an area dragged over, then pastes it wherever clicked. */
    COPY("brush_copy", "hint_copy", null),
    TREES("brush_trees", "hint_trees", Resource.TREE),
    PALMS("brush_palms", "hint_trees", Resource.PALM),
    ROCK("brush_rock", "hint_rock", Resource.ROCK),
    IRON("brush_iron", "hint_iron", Resource.IRON),
    /** Clears every kind of resource under the brush. */
    ERASE("brush_erase", "hint_erase", null),
    /** Puts a player's spawn where clicked, or takes one away. */
    SPAWN("tool_spawns", "hint_spawn", null);

    private final @NonNull String name_key;
    private final @NonNull String hint_key;
    private final @Nullable Resource resource;

    Brush(@NonNull String name_key, @NonNull String hint_key, @Nullable Resource resource) {
        this.name_key = name_key;
        this.hint_key = hint_key;
        this.resource = resource;
    }

    /** The name, which for trees depends on the island's terrain: jungle and palm, or oak and pine. */
    @NonNull
    String getName(Landscape.@NonNull TerrainType terrain) {
        if (resource == Resource.TREE || resource == Resource.PALM)
            return MapEditor.i18n(name_key + "_" + terrain.name().toLowerCase(java.util.Locale.ROOT));
        return MapEditor.i18n(name_key);
    }

    @NonNull
    String getHint() {
        return MapEditor.i18n(hint_key);
    }

    /**
     * Ramps and isthmuses are laid in one go when the drag ends, and the area to copy is picked by dragging over it.
     * Brushes that are neither a drag nor a course paint while the button is held.
     */
    boolean isDragShape() {
        return this == RAMP || this == ISTHMUS || this == COPY;
    }

    /** Paths, rivers and ridges follow a course clicked out a point at a time, and are laid when it is done. */
    boolean isCourse() {
        return this == PATH || this == RIVER || this == MOUNTAIN;
    }

    /** Whether this is a terrain brush, from the terrain dropdown. */
    boolean isTerrainBrush() {
        return !isResourceBrush() && this != SPAWN;
    }

    /** Whether this brush works on resources rather than on the terrain. */
    boolean isResourceBrush() {
        return resource != null || this == ERASE;
    }

    /** The resource this brush paints, or null for a terrain brush or the eraser. */
    @Nullable
    Resource getResource() {
        return resource;
    }
}
