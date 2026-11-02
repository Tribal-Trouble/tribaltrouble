package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.landscape.AbstractTreeGroup.TreeType;
import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;

/** The resources an island is generated with, one kind per list the generator hands the world. */
enum Resource {
    /** Jungle trees, or oaks on a viking island. */
    TREE,
    /** Palm trees, or pines on a viking island. */
    PALM,
    ROCK,
    IRON;

    boolean isTree() {
        return this == TREE || this == PALM;
    }

    /** The tree kind planted for this resource on the given terrain, as AbstractTreeGroup.newRoot picks it. */
    @NonNull
    TreeType treeType(Landscape.@NonNull TerrainType terrain) {
        return switch (this) {
            case TREE -> terrain == Landscape.TerrainType.NATIVE ? TreeType.JUNGLE : TreeType.OAK;
            case PALM -> terrain == Landscape.TerrainType.NATIVE ? TreeType.PALM : TreeType.PINE;
            default -> throw new IllegalStateException(this + " is not a tree");
        };
    }

    static @NonNull Resource ofTree(@NonNull TreeType type) {
        return type == TreeType.JUNGLE || type == TreeType.OAK ? TREE : PALM;
    }
}
