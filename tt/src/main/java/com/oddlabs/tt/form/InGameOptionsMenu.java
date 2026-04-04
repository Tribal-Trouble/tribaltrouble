package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class InGameOptionsMenu extends AbstractOptionsMenu {
    private final @NonNull WorldViewer viewer;

    public InGameOptionsMenu(@NonNull GUIRoot gui_root, @NonNull WorldViewer viewer) {
        super(gui_root);
        this.viewer = viewer;
        chooseGamespeed(getGamespeed());
    }

    private int getGamespeed() {
        int gamespeed = viewer.getLocalPlayer().getGamespeed();
        if (!World.isValidGamespeed(gamespeed))
            gamespeed = viewer.getWorld().getGamespeed();
        return gamespeed;
    }

    @Override
    protected void changeGamespeed(int index) {
        super.changeGamespeed(index);
        viewer.getPeerHub().getPlayerInterface().setPreferredGamespeed(index);
    }
}
