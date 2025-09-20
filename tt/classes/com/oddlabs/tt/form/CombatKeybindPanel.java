package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;

import java.util.Arrays;
import java.util.List;

/**
 * Combat keybinds panel containing all military and combat-related controls.
 * Includes army groups, chieftain magic, and tower actions.
 */
public class CombatKeybindPanel extends AbstractKeybindPanel {

    public CombatKeybindPanel(GUIRoot gui_root, String caption) {
        super(gui_root, caption);
    }

    @Override
    protected List<Section> getSections() {
    // Swap Chieftain and Tower sections: Chieftain Magic first, then Tower Actions, then Army Groups
    return Arrays.asList(
        sec(
            "Chieftain Magic",
            Globals.KB_CHIEFTAIN_MAGIC1,
            Globals.KB_CHIEFTAIN_MAGIC2),
        sec("Tower Actions", Globals.KB_TOWER_ATTACK, Globals.KB_TOWER_EXIT),
        sec(
            "Army Groups",
            Globals.KB_ARMY_GROUP_0,
            Globals.KB_ARMY_GROUP_1,
            Globals.KB_ARMY_GROUP_2,
            Globals.KB_ARMY_GROUP_3,
            Globals.KB_ARMY_GROUP_4,
            Globals.KB_ARMY_GROUP_5,
            Globals.KB_ARMY_GROUP_6,
            Globals.KB_ARMY_GROUP_7,
            Globals.KB_ARMY_GROUP_8,
            Globals.KB_ARMY_GROUP_9)
    );
    }
}