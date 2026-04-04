package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface MouseWheelListener extends EventListener {
    void mouseScrolled(int amount);
}
