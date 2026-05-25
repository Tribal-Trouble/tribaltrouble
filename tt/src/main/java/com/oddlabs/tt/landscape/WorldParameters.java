package com.oddlabs.tt.landscape;

import com.oddlabs.matchmaking.Game;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class WorldParameters implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final @NonNull String map_code;
    private final int initial_unit_count;
    private final int max_unit_count;
    private final int initial_game_speed;
    private final int map_size;

    public WorldParameters(int initial_game_speed, @NonNull String map_code, int initial_unit_count,
            int max_unit_count) {
        this(initial_game_speed, map_code, initial_unit_count, max_unit_count, Game.SIZE_NONE);
    }

    public WorldParameters(int initial_game_speed, @NonNull String map_code, int initial_unit_count, int max_unit_count,
            int map_size) {
        this.map_code = map_code;
        this.initial_unit_count = initial_unit_count;
        this.max_unit_count = max_unit_count;
        this.initial_game_speed = initial_game_speed;
        this.map_size = map_size;
    }

    public @NonNull String getMapcode() {
        return map_code;
    }

    public int getInitialUnitCount() {
        return initial_unit_count;
    }

    public int getMaxUnitCount() {
        return max_unit_count;
    }

    public int getInitialGameSpeed() {
        return initial_game_speed;
    }

    public int getMapSize() {
        return map_size;
    }
}
