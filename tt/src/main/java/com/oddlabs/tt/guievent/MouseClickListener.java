package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.MouseButton;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface MouseClickListener extends EventListener {
    void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks);
}
