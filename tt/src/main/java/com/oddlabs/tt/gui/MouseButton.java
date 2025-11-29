package com.oddlabs.tt.gui;

import org.jspecify.annotations.Nullable;

public enum MouseButton {
    LEFT,
    RIGHT,
    MIDDLE;

    public static @Nullable MouseButton fromInt(int button) {
        return switch (button) {
            case 0 -> LEFT;
            case 1 -> RIGHT;
            case 2 -> MIDDLE;
            default -> null;
        };
    }
}
