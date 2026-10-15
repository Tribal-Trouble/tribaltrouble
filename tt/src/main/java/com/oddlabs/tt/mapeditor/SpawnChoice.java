package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.procedural.LandscapeOverride;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** How the players of a game on a custom map start, when the map has spawns. */
enum SpawnChoice {
    /** Each player at its own slot's spawn: player 1 at spawn 1 and so on. */
    IN_ORDER("spawns_in_order"),
    /** The players at the map's spawns in an order of chance. */
    SHUFFLED("spawns_shuffled"),
    /** At the places the game picks, as on a map without spawns. */
    RANDOM("spawns_random");

    private final @NonNull String key;

    SpawnChoice(@NonNull String key) {
        this.key = key;
    }

    @NonNull
    String getLabel() {
        return MapEditor.i18n(key);
    }

    /**
     * Where the island generator starts the players.
     *
     * @param seed the order of chance of shuffled spawns, which the generator carries to every player in the game
     * @return the spawns, or null to let the game pick every place
     */
    LandscapeOverride.@Nullable Spawns apply(@NonNull Spawns spawns, long seed) {
        if (this == RANDOM || spawns.isEmpty())
            return null;
        return spawns.toOverride(this == SHUFFLED, seed);
    }
}
