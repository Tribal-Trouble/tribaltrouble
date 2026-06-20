package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Color vision deficiency (CVD) correction modes.
 */
public enum CVDMode {
    NONE(0),
    PROTANOPIA(1),
    DEUTERANOPIA(2),
    TRITANOPIA(3);

    private final int value;

    private static final Set<CVDMode> VALUES = EnumSet.allOf(CVDMode.class);

    CVDMode(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this CVD mode.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link CVDMode} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link CVDMode}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static @NonNull CVDMode fromValue(int value) {
        for (CVDMode mode : VALUES) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown CVD mode value: " + value);
    }
}
