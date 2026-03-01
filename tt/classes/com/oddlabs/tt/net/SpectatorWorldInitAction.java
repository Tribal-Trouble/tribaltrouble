package com.oddlabs.tt.net;

import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.viewer.WorldViewer;

public final strictfp class SpectatorWorldInitAction implements WorldInitAction {
    public void run(WorldViewer viewer) {
        Menu.completeGameSetupHack(viewer);
        viewer.getDelegate().setObserverMode();
    }
}
