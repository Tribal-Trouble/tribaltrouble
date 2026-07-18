package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class IconSpinnerButton extends NonFocusIconButton {
    private final IconSpinner owner;

    public IconSpinnerButton(@NonNull ModeIconQuads icon_quad, @Nullable GameAction action,
            @NonNull Supplier<@NonNull String> tool_tip,
            IconSpinner owner) {
        super(icon_quad, action, tool_tip);
        this.owner = owner;
    }

    @Override
    public boolean hasToolTip() {
        return isDisabled() ? owner.hasToolTip() : super.hasToolTip();
    }

    @Override
    public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        if (isDisabled())
            owner.appendToolTip(tool_tip_box);
        else
            super.appendToolTip(tool_tip_box);
    }
}
