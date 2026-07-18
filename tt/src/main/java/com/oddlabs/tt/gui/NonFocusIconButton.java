package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class NonFocusIconButton extends IconButton {
    public NonFocusIconButton(@NonNull ModeIconQuads icon, @Nullable GameAction action,
            @NonNull Supplier<@NonNull String> tool_tip) {
        super(icon, action, tool_tip);
    }

    @Override
    public final void setFocus() {
        // we don't want to be focused
    }
}
