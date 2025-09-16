package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;

import java.util.Arrays;
import java.util.List;

/**
 * Essential keybinds panel containing the most commonly used gameplay controls.
 * Includes basic unit actions, building construction, and general gameplay shortcuts.
 */
public class EssentialKeybindPanel extends AbstractKeybindPanel {

    public EssentialKeybindPanel(GUIRoot gui_root, String caption) {
        super(gui_root, caption);
    }

    @Override
    protected List<Section> getSections() {
        return Arrays.asList(
                sec(
                        "Basic Unit Actions",
                        Globals.KB_MOVE,
                        Globals.KB_ATTACK,
                        Globals.KB_GATHER_REPAIR),
                sec(
                        "Building Construction",
                        Globals.KB_BUILD_ARMORY,
                        Globals.KB_BUILD_QUARTERS,
                        Globals.KB_BUILD_TOWER),
                sec(
                        "General Gameplay",
                        Globals.KB_TOGGLE_MAP_MODE,
                        Globals.KB_JUMP_TO_NOTIFICATION,
                        Globals.KB_PLACE_BEACON,
                        Globals.KB_NEXT_IDLE_PEON)
        );
    }
}