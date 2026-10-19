package com.oddlabs.tt.resource;

import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public interface WorldGenerator extends Serializable {
    /** Terrain, supplies, grids and start positions. CPU only; safe without a GL context. */
    @NonNull
    WorldInfo generateTerrain(int num_players, int initial_unit_count, float random_start_pos);

    /** Bakes the colour and detail textures for terrain from {@link #generateTerrain}. Needs a GL context. */
    @NonNull
    WorldInfo bakeTextures(@NonNull WorldInfo terrain);

    @NonNull
    default WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos) {
        return bakeTextures(generateTerrain(num_players, initial_unit_count, random_start_pos));
    }

    Landscape.@NonNull TerrainType getTerrainType();

    int getMetersPerWorld();

    @NonNull
    FogInfo getFogInfo();
}
