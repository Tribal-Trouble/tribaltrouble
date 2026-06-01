package com.oddlabs.matchmaking;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * World-level options a preset captures: the terrain and pacing that exist in every mode. Mode-specific options live in
 * {@link GameModeOptions} instead. Stored as raw UI indices/values so {@code TerrainMenu} can stamp them straight back
 * onto its pulldowns and sliders.
 */
@JsonDeserialize(builder = WorldConfig.Builder.class)
public final class WorldConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int gamespeed;
    private final int island_size;
    private final int terrain_type;
    private final int hills;
    private final int vegetation;
    private final int supplies;

    private WorldConfig(@NonNull Builder b) {
        this.gamespeed = b.gamespeed;
        this.island_size = b.island_size;
        this.terrain_type = b.terrain_type;
        this.hills = b.hills;
        this.vegetation = b.vegetation;
        this.supplies = b.supplies;
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public static @NonNull WorldConfig defaults() {
        return new Builder().build();
    }

    public int getGamespeed() {
        return gamespeed;
    }

    public int getIslandSize() {
        return island_size;
    }

    public int getTerrainType() {
        return terrain_type;
    }

    public int getHills() {
        return hills;
    }

    public int getVegetation() {
        return vegetation;
    }

    public int getSupplies() {
        return supplies;
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    public static final class Builder {
        private int gamespeed;
        private int island_size;
        private int terrain_type;
        private int hills;
        private int vegetation;
        private int supplies;

        private Builder() {
        }

        public @NonNull Builder gamespeed(int gamespeed) {
            this.gamespeed = gamespeed;
            return this;
        }

        public @NonNull Builder islandSize(int island_size) {
            this.island_size = island_size;
            return this;
        }

        public @NonNull Builder terrainType(int terrain_type) {
            this.terrain_type = terrain_type;
            return this;
        }

        public @NonNull Builder hills(int hills) {
            this.hills = hills;
            return this;
        }

        public @NonNull Builder vegetation(int vegetation) {
            this.vegetation = vegetation;
            return this;
        }

        public @NonNull Builder supplies(int supplies) {
            this.supplies = supplies;
            return this;
        }

        public @NonNull WorldConfig build() {
            return new WorldConfig(this);
        }
    }
}
