package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Playable unit types in the game.
 * Supports mapping to legacy serialization integer values.
 */
public enum UnitType {
    WARRIOR_ROCK(0),
    WARRIOR_IRON(1),
    WARRIOR_RUBBER(2),
    PEON(3),
    CHIEFTAIN(4);

    private final int value;

    private static final Set<UnitType> VALUES = EnumSet.allOf(UnitType.class);

    UnitType(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this unit type.
     * Used to maintain compatibility with legacy serialization and network protocols.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link UnitType} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link UnitType}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static @NonNull UnitType fromValue(int value) {
        for (UnitType type : VALUES) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown legacy unit type value: " + value);
    }
}
