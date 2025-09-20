package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;

import java.util.Arrays;
import java.util.List;

/**
 * System keybinds panel containing interface controls and camera management.
 * Includes system/interface shortcuts and all camera control bindings.
 */
public class SystemKeybindPanel extends AbstractKeybindPanel {

    public SystemKeybindPanel(GUIRoot gui_root, String caption) {
        super(gui_root, caption);
    }

    @Override
    protected List<Section> getSections() {
    return Arrays.asList(
        // Swap order: Camera Controls first, then System / Interface
        sec(
            "Camera Controls",
            Globals.KB_PAN_CAMERA_LEFT,
            Globals.KB_PAN_CAMERA_RIGHT,
            Globals.KB_PAN_CAMERA_UP,
            Globals.KB_PAN_CAMERA_DOWN,
            Globals.KB_CAMERA_ZOOM_IN,
            Globals.KB_CAMERA_ZOOM_OUT,
            Globals.KB_CAMERA_ROTATE_LEFT,
            Globals.KB_CAMERA_ROTATE_RIGHT,
            Globals.KB_CAMERA_PITCH_UP,
            Globals.KB_CAMERA_PITCH_DOWN,
            // Keyboard equivalents for mouse gestures
            Globals.KB_CAMERA_ZOOM_HOLD,
            Globals.KB_CAMERA_FIRST_PERSON_TOGGLE),
        sec(
            "System / Interface",
            Globals.KB_CHAT_TOGGLE,
            Globals.KB_BACK_CANCEL,
            Globals.KB_GAMESPEED_INCREASE,
            Globals.KB_GAMESPEED_DECREASE,
            Globals.KB_PAUSE)
    );
    }
}