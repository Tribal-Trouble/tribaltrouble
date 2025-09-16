package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;

import java.util.Arrays;
import java.util.List;

/**
 * Map Editor keybinds panel exposing editor-only keyboard shortcuts.
 * Includes tool selection, overlay/toolbar toggles, file commands, and polyline helpers.
 */
public class EditorKeybindPanel extends AbstractKeybindPanel {

    public EditorKeybindPanel(GUIRoot gui_root, String caption) {
        super(gui_root, caption);
    }

    @Override
    protected List<Section> getSections() {
        return Arrays.asList(
                sec(
                        "Tools",
                        Globals.KB_EDITOR_SET_TERRAIN_TOOL,
                        Globals.KB_EDITOR_SET_RESOURCE_TOOL),
                sec(
                        "Overlays & UI",
                        Globals.KB_EDITOR_OVERLAY_MODE,
                        Globals.KB_EDITOR_TOGGLE_TOOLBAR,
                        Globals.KB_TOGGLE_MAP_MODE // reuse from game for map-fit toggle
                        ),
                sec(
                        "File",
                        Globals.KB_EDITOR_SAVE,
                        Globals.KB_EDITOR_LOAD,
                        Globals.KB_EDITOR_TEST_MAP),
                sec(
                        "Polyline Editing",
                        Globals.KB_EDITOR_POLY_APPLY,
                        Globals.KB_EDITOR_POLY_UNDO_POINT)
        );
    }
}
