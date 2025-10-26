package com.oddlabs.tt.model;

import org.jspecify.annotations.Nullable;

public abstract class UnitContainer extends SupplyContainer {
	public UnitContainer(int capacity) {
		super(capacity);
	}

	public abstract void enter(Unit unit);
	public abstract boolean canEnter(Unit unit);
	public abstract @Nullable Unit exit();
	public abstract void animate(float t);
}
