package com.oddlabs.tt.camera;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;

/** What the map mode camera needs from the screen that switched to it. */
public interface MapCameraOwner {
    @NonNull
    GUIRoot getGUIRoot();

    /** Shows the map mode caption. */
    void addChild(@NonNull GUIObject child);

    /** Called once the camera is back where map mode started, to switch back to the normal camera. */
    void exitMapMode();

    /** The highest the view may land after jumping to a spot on the map. */
    default float getMaxCameraZ() {
        return GameCamera.MAX_Z;
    }
}
