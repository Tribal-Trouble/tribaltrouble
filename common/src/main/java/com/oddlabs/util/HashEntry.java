package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

public final class HashEntry<T> extends ListElementImpl<HashEntry<T>> {
	private @NonNull T hash_entry;
	private final int key;

	public HashEntry(int key, @NonNull T entry) {
		this.key = key;
		this.hash_entry = entry;
	}

    @Override
    protected @NonNull HashEntry<T> self() {
        return this;
    }

    public @NonNull T getEntry() {
		return hash_entry;
	}

	public @NonNull T setEntry(@NonNull T entry) {
		T old = hash_entry;
		hash_entry = entry;
		return old;
	}

	public int getKey() {
		return key;
	}
}
