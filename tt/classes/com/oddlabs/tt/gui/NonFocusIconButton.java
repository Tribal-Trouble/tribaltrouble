package com.oddlabs.tt.gui;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Quad;

import java.util.ResourceBundle;

public strictfp class NonFocusIconButton extends IconButton implements ToolTip {
    private final String tool_tip;
    private final ResourceBundle bundle;
    private final String tooltip_id;
    private final String keybind_action;

    public NonFocusIconButton(Quad[] icon_quad, String tool_tip) {
        super(icon_quad);
        this.tool_tip = tool_tip;
        this.bundle = null;
        this.tooltip_id = null;
        this.keybind_action = null;
    }

    public NonFocusIconButton(
            Quad[] icon_quad, ResourceBundle bundle, String tooltip_id, String keybind_action) {
        super(icon_quad);
        this.tool_tip = null;
        this.bundle = bundle;
        this.tooltip_id = tooltip_id;
        this.keybind_action = keybind_action;
    }

    public void appendToolTip(ToolTipBox tool_tip_box) {
        if (keybind_action != null) {
            String shortcut = Settings.getSettings().getKeybindString(keybind_action);
            tool_tip_box.append(Utils.getBundleString(bundle, tooltip_id, new Object[] {shortcut}));
        } else {
            tool_tip_box.append(tool_tip);
        }
    }

    public final void setFocus() {}
}
