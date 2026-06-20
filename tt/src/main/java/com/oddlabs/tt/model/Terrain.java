package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

/**
 * Landscape terrain types.
 */
public enum Terrain {
    NATIVE(0),
    VIKING(1);

    private final int value;

    Terrain(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static @NonNull Terrain fromValue(int value) {
        for (Terrain type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown terrain value: " + value);
    }
}
