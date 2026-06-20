package com.oddlabs.tt.guievent;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface MouseHorizontalWheelListener extends EventListener {
    void mouseScrolledHorizontally(int amount);
}