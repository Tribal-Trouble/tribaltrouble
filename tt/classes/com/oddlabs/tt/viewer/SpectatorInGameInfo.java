package com.oddlabs.tt.viewer;

public final strictfp class SpectatorInGameInfo extends DefaultInGameInfo {
    private final float random_start_position;

    public SpectatorInGameInfo(float random_start_position) {
        this.random_start_position = random_start_position;
    }

    public boolean isMultiplayer() {
        return true;
    }

    public float getRandomStartPosition() {
        return random_start_position;
    }
}
