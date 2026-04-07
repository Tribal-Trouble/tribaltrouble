package com.oddlabs.tt.net;

import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.viewer.WorldViewer;

public final class SpectatorWorldInitAction implements WorldInitAction {
    @Override
    public void run(WorldViewer viewer) {
        Menu.completeGameSetupHack(viewer);
        viewer.getDelegate().setObserverMode();
    }
}
