package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Game speed options.
 */
public enum Gamespeed {
    PAUSE(0),
    SLOW(1),
    NORMAL(2),
    FAST(3),
    LUDICROUS(4);

    private final int value;

    private static final Set<Gamespeed> VALUES = EnumSet.allOf(Gamespeed.class);

    Gamespeed(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this gamespeed.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link Gamespeed} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link Gamespeed}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static @NonNull Gamespeed fromValue(int value) {
        for (Gamespeed speed : VALUES) {
            if (speed.value == value) {
                return speed;
            }
        }
        throw new IllegalArgumentException("Unknown gamespeed value: " + value);
    }
}
