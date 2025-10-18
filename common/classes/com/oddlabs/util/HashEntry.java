package com.oddlabs.util;

public final class HashEntry<T> extends ListElementImpl<T> {
	private T hash_entry;
	private final int key;

	public HashEntry(int key, T entry) {
		this.key = key;
		this.hash_entry = entry;
	}

	public T getEntry() {
		return hash_entry;
	}

	public T setEntry(T entry) {
		T old = hash_entry;
		hash_entry = entry;
		return old;
	}

	public int getKey() {
		return key;
	}
}
