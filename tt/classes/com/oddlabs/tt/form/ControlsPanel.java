package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;

/**
 * Unified controls panel that contains sub-tabs for keybinds and camera settings. This panel groups
 * all input-related configuration options in one place.
 */
public class ControlsPanel extends Panel {
    private final PanelGroup controlsGroup;

    public ControlsPanel(GUIRoot gui_root, String caption) {
        super(caption);

        // Create sub-panels for keybinds and camera settings
        Panel keybindsPanel = new KeybindPanel(gui_root, "Keybinds");
        Panel cameraPanel = new CameraPanel(gui_root, "Camera");

        // Create internal PanelGroup for sub-tabs
        // Keybinds first since they're more commonly accessed
        Panel[] subPanels = {keybindsPanel, cameraPanel};
        controlsGroup = new PanelGroup(subPanels, 0);

        addChild(controlsGroup);
        controlsGroup.place();
        compileCanvas();
    }

    @Override
    public void onActivated() {
        // The PanelGroup will handle activation of the currently selected sub-panel
        // This ensures that when the Controls tab becomes active, the current sub-tab
        // also gets properly refreshed
    }
}
