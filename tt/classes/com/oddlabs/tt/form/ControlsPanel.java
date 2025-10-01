package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

/**
 * Unified controls panel that contains sub-tabs for keybinds and camera settings. This panel groups
 * all input-related configuration options in one place.
 */
public class ControlsPanel extends Panel {
    private static final ResourceBundle bundle =
            ResourceBundle.getBundle(ControlsPanel.class.getName());
    private final PanelGroup controlsGroup;

    public ControlsPanel(GUIRoot gui_root, String caption) {
        super(caption);

        // Create sub-panels for keybinds and camera settings
        Panel keybindsPanel =
                new KeybindPanel(
                        gui_root, Utils.getBundleString(bundle, "keybinds_tab_caption"));
        Panel cameraPanel =
                new CameraPanel(
                        gui_root, Utils.getBundleString(bundle, "camera_tab_caption"));

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
