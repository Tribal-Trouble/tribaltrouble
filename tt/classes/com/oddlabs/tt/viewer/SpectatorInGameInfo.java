package com.oddlabs.tt.viewer;

import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.gui.Group;

public final strictfp class SpectatorInGameInfo implements InGameInfo {
    public void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos) {}

    public void addGameOverGUI(
            WorldViewer viewer, GameStatsDelegate delegate, int header_y, Group buttons) {}

    public void abort(WorldViewer viewer) {}

    public void close(WorldViewer viewer) {}

    public boolean isMultiplayer() {
        return true;
    }

    public boolean isRated() {
        return false;
    }

    public float getRandomStartPosition() {
        return 0f;
    }
}
