package com.oddlabs.tt.resource;

import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public interface WorldGenerator extends Serializable {
    @NonNull
    WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos);

    Landscape.@NonNull TerrainType getTerrainType();

    int getMetersPerWorld();

    @NonNull
    FogInfo getFogInfo();
}
