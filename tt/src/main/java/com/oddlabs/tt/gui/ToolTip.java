package com.oddlabs.tt.gui;

public interface ToolTip {
    void appendToolTip(ToolTipBox tool_tip);

    default boolean hasToolTip() {
        return true;
    }
}
