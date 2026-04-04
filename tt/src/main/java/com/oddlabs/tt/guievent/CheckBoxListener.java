package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface CheckBoxListener extends EventListener {
    void checked(boolean marked);
}
