package com.oddlabs.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class ListElementImpl<T extends ListElementImpl<T>> implements ListElement<T> {
	private @Nullable LinkedList<T> parent;

	private @Nullable T next;
	private @Nullable T prior;

    protected abstract @NonNull T self();

	@Override
	public final void setListOwner(@Nullable LinkedList<T> owner) {
		parent = owner;
	}

	@Override
	public final @Nullable LinkedList<T> getListOwner() {
		return parent;
	}

	@Override
	public final void setPrior(@Nullable T prior) {
		this.prior = prior;
	}

	@Override
	public final void setNext(@Nullable T next) {
		this.next = next;
	}

	@Override
	public final @Nullable T getPrior() {
		return prior;
	}

	@Override
	public final @Nullable T getNext() {
		return next;
	}
}
