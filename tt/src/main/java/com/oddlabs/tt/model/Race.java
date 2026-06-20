package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Playable races in the game.
 */
public enum Race {
    NATIVES(0),
    VIKINGS(1);

    private final int value;

    private static final Set<Race> VALUES = EnumSet.allOf(Race.class);

    Race(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this race type.
     * Used to maintain compatibility with legacy serialization and network protocols.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link Race} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link Race}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static @NonNull Race fromValue(int value) {
        for (Race type : VALUES) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown legacy race value: " + value);
    }
}
