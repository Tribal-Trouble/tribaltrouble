package com.oddlabs.tt.gui;

import com.oddlabs.tt.util.ToolTip;
import org.jspecify.annotations.NonNull;

public class NonFocusIconButton extends IconButton implements ToolTip {
    private final @NonNull String tool_tip;

    public NonFocusIconButton(@NonNull ModeIconQuads icon, @NonNull String tool_tip) {
        super(icon);
        this.tool_tip = tool_tip;
    }

    @Override
    public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        tool_tip_box.append(tool_tip);
    }

    @Override
    public final void setFocus() {
    }
}
