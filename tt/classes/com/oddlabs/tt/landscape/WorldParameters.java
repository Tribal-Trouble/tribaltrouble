package com.oddlabs.tt.landscape;

public final strictfp class WorldParameters {
    private final String map_code;
    private final int initial_unit_count;
    private final int max_unit_count;
    private final int initial_game_speed;
    private final GameModeOptions game_mode;

    public WorldParameters(
            int initial_game_speed, String map_code, int initial_unit_count, int max_unit_count) {
        this.map_code = map_code;
        this.initial_unit_count = initial_unit_count;
        this.max_unit_count = max_unit_count;
        this.initial_game_speed = initial_game_speed;
        this.game_mode = GameModeOptions.defaults(com.oddlabs.tt.player.Player.MAX_BUILDING_COUNT);
    }

    public WorldParameters(
            int initial_game_speed,
            String map_code,
            int initial_unit_count,
            int max_unit_count,
            GameModeOptions options) {
        this.map_code = map_code;
        this.initial_unit_count = initial_unit_count;
        this.max_unit_count = max_unit_count;
        this.initial_game_speed = initial_game_speed;
        this.game_mode = options == null
                ? GameModeOptions.defaults(com.oddlabs.tt.player.Player.MAX_BUILDING_COUNT)
                : options;
    }

    public final String getMapcode() {
        return map_code;
    }

    public final int getInitialUnitCount() {
        return initial_unit_count;
    }

    public final int getMaxUnitCount() {
        return max_unit_count;
    }

    public final int getInitialGameSpeed() {
        return initial_game_speed;
    }

    public final GameModeOptions getGameMode() {
        return game_mode;
    }
}
