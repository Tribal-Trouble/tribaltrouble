package com.oddlabs.tt.camera;

import com.oddlabs.tt.gui.Renderable;

/**
 * Minimal host for MapCamera so it can be reused outside SelectionDelegate.
 * Provides only what MapCamera needs: adding its overlay label and exiting map mode.
 */
public interface MapModeHost {
    void addChild(Renderable renderable);
    void exitMapMode();
}
