package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Logical identifiers for resource supply types in the simulation.
 */
public enum SupplyType {
    WOOD(TreeSupply.class),
    ROCK(RockSupply.class),
    IRON(IronSupply.class),
    RUBBER(RubberSupply.class);

    private final @NonNull Class<? extends Supply> supplyClass;

    SupplyType(@NonNull Class<? extends Supply> supplyClass) {
        this.supplyClass = supplyClass;
    }

    public @NonNull Class<? extends Supply> getSupplyClass() {
        return supplyClass;
    }

    public static @Nullable SupplyType fromClass(@NonNull Class<?> cl) {
        if (cl == TreeSupply.class) {
            return WOOD;
        } else if (cl == RockSupply.class) {
            return ROCK;
        } else if (cl == IronSupply.class) {
            return IRON;
        } else if (cl == RubberSupply.class) {
            return RUBBER;
        }
        return null;
    }
}
