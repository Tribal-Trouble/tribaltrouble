package com.oddlabs.tt.resource;

import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public interface WorldGenerator extends Serializable {
    @NonNull
    WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos);

    /**
     * Builds the world for players in the given lobby slots, which a map may give starting places of their own.
     *
     * @param slots the lobby slot of each player taking part, in the order the world has them
     */
    default @NonNull WorldInfo generate(int @NonNull [] slots, int initial_unit_count, float random_start_pos) {
        return generate(slots.length, initial_unit_count, random_start_pos);
    }

    Landscape.@NonNull TerrainType getTerrainType();

    int getMetersPerWorld();

    @NonNull
    FogInfo getFogInfo();
}
