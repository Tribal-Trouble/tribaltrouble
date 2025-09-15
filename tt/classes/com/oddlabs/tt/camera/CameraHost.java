package com.oddlabs.tt.camera;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.Picker;

/**
 * Minimal host interface for GameCamera. Implemented by WorldViewer and editor adapter.
 */
public interface CameraHost {
    World getWorld();
    Picker getPicker();
    GUIRoot getGUIRoot();
}
