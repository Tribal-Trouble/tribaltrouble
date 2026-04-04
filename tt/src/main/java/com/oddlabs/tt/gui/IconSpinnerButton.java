package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public final class IconSpinnerButton extends NonFocusIconButton {
    private final IconSpinner owner;

    public IconSpinnerButton(@NonNull ModeIconQuads icon_quad, @NonNull String tool_tip, IconSpinner owner) {
        super(icon_quad, tool_tip);
        this.owner = owner;
    }

    @Override
    public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        if (isDisabled())
            owner.appendToolTip(tool_tip_box);
        else
            super.appendToolTip(tool_tip_box);
    }
}
