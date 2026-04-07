package com.oddlabs.tt.viewer;

public final class SpectatorInGameInfo extends DefaultInGameInfo {
    private final float random_start_position;

    public SpectatorInGameInfo(float random_start_position) {
        this.random_start_position = random_start_position;
    }

    @Override
    public boolean isMultiplayer() {
        return true;
    }

    @Override
    public float getRandomStartPosition() {
        return random_start_position;
    }
}
