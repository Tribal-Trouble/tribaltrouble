package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class UnitContainer extends SupplyContainer {
    public UnitContainer(int capacity) {
        super(capacity);
    }

    public abstract void enter(@NonNull Unit unit);

    public abstract boolean canEnter(@NonNull Unit unit);

    public abstract @Nullable Unit exit();

    public void animate(float t) {
    }
}
