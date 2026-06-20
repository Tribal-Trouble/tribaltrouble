package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

/**
 * Factory class for constructing UnitSupplyContainers.
 */
public final class UnitSupplyContainerFactory extends SupplyContainerFactory {

    public UnitSupplyContainerFactory(int max_resource_count) {
        super(max_resource_count);
    }

    @Override
    public @NonNull SupplyContainer createContainer(Selectable<?> selectable) {
        return new UnitSupplyContainer(getMaxResourceCount());
    }
}
