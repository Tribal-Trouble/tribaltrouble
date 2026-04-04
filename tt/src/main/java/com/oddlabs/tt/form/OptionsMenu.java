package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;

public final class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(@NonNull GUIRoot gui_root) {
        super(gui_root);
        chooseGamespeed(Globals.gamespeed);
    }
}
