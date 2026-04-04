package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class ReproduceUnitContainerFactory implements UnitContainerFactory {
    @Override
    public @NonNull UnitContainer createContainer(@NonNull Building building) {
        return new ReproduceUnitContainer(building);
    }
}
