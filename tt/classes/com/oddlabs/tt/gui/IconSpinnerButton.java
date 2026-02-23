package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;

import java.util.ResourceBundle;

public final strictfp class IconSpinnerButton extends NonFocusIconButton {
    private final IconSpinner owner;

    public IconSpinnerButton(
            Quad[] icon_quad,
            ResourceBundle bundle,
            String tooltip_id,
            String keybind_action,
            IconSpinner owner) {
        super(icon_quad, bundle, tooltip_id, keybind_action);
        this.owner = owner;
    }

    public final void appendToolTip(ToolTipBox tool_tip_box) {
        if (isDisabled()) owner.appendToolTip(tool_tip_box);
        else super.appendToolTip(tool_tip_box);
    }
}
