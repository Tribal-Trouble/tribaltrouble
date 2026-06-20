package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the types of constructible buildings in the game.
 * Supports mapping to legacy serialization integer values.
 */
public enum BuildingType {
    QUARTERS(0),
    ARMORY(1),
    TOWER(2);

    private final int value;

    private static final Set<BuildingType> VALUES = EnumSet.allOf(BuildingType.class);

    BuildingType(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this building type.
     * Used to maintain compatibility with legacy serialization and network protocols.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link BuildingType} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link BuildingType}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static @NonNull BuildingType fromValue(int value) {
        for (BuildingType type : VALUES) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown legacy building type value: " + value);
    }
}
