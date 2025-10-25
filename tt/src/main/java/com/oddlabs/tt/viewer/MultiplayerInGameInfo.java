package com.oddlabs.tt.viewer;

import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.gui.FreeQuitLabel;
import com.oddlabs.tt.gui.Group;

public final class MultiplayerInGameInfo extends DefaultInGameInfo {

    private final float random_start_position;
    private final boolean is_rated;

    public MultiplayerInGameInfo(float random_start_position, boolean is_rated) {
        this.random_start_position = random_start_position;
        this.is_rated = is_rated;
    }

    @Override
    public void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos) {
        super.addGUI(viewer, menu, game_infos);
        FreeQuitLabel free_quit_label = new FreeQuitLabel(viewer.getWorld(), viewer.getAnimationManagerLocal());
        menu.addChild(free_quit_label);
        free_quit_label.setPos(0, 0);
    }

    @Override
    public void addGameOverGUI(WorldViewer viewer, GameStatsDelegate delegate, int header_y, Group buttons) {
        addGameOverGUI(viewer, delegate, header_y, buttons, false);
    }

    @Override
    public boolean isRated() {
        return is_rated;
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
