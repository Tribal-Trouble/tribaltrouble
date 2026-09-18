package com.oddlabs.tt.net;

import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.viewer.WorldViewer;

public final class SpectatorWorldInitAction implements WorldInitAction {
    private final int followed_slot;

    public SpectatorWorldInitAction(int followed_slot) {
        this.followed_slot = followed_slot;
    }

    @Override
    public void run(WorldViewer viewer) {
        Menu.completeGameSetupHack(viewer);
        viewer.getDelegate().setObserverMode();
        if (viewer.getObserverView() != null)
            viewer.getObserverView().follow(followed_slot);
    }
}
