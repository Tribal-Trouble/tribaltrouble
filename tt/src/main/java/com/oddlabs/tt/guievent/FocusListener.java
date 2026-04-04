package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface FocusListener extends EventListener {
    void activated(boolean activated);
}
