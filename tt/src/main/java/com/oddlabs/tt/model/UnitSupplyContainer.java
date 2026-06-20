package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A specialized supply container for peon units to carry resources.
 */
public final class UnitSupplyContainer extends SupplyContainer {
    private @Nullable SupplyType type;

    public UnitSupplyContainer(int max_resource_count) {
        super(max_resource_count);
    }

    @Override
    public int increaseSupply(int amount) {
        throw new UnsupportedOperationException("UnitSupplyContainer requires a supply type");
    }

    public int increaseSupply(int amount, @NonNull SupplyType type) {
        if (this.type != type) {
            this.type = type;
            super.increaseSupply(-getNumSupplies());
        }
        return super.increaseSupply(amount);
    }

    public @NonNull Optional<SupplyType> getSupplyType() {
        return Optional.ofNullable(type);
    }
}
