package com.oddlabs.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Integer to object map
 */
public final class HashTable<T> {
	private static final int DEFAULT_INITIAL_ENTRIES = 10;
	private static final int DEFAULT_MUL_FACTOR = 2;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	@SuppressWarnings("unchecked")
    private @Nullable LinkedList<@NonNull HashEntry<T>> @NonNull [] entries = new LinkedList[DEFAULT_INITIAL_ENTRIES];
	private final float load_factor = DEFAULT_LOAD_FACTOR;
	private final int mul_factor = DEFAULT_MUL_FACTOR;
    private int num_entries;

	public HashTable() {
	}

	public int size() {
		return num_entries;
	}

	private int hash(int key) {
		int hash = key % entries.length;
        return hash >= 0 ? hash : hash + entries.length;
	}

	public @Nullable T put(int key, @NonNull T val) {
		int hash = hash(key);
		if (entries[hash] == null) {
			entries[hash] = new LinkedList<>();
		} else {
			HashEntry<T> current_entry = entries[hash].getFirst();
			while (current_entry != null) {
				int current_key = current_entry.getKey();
				if (current_key == key) {
					T result = current_entry.setEntry(val);
					return result;
				}
				current_entry = current_entry.getNext();
			}
		}
		HashEntry<T> hash_entry = new HashEntry<>(key, val);
		entries[hash].addLast(hash_entry);
		num_entries++;
		if (num_entries > load_factor*entries.length)
			rehash();
		return null;
	}

	public @Nullable T get(int key) {
		int hash = hash(key);
		if (entries[hash] == null)
			return null;
		HashEntry<T> current_entry = entries[hash].getFirst();
		while (current_entry != null) {
			int current_key = current_entry.getKey();
			if (current_key == key)
				return current_entry.getEntry();
			current_entry = current_entry.getNext();
		}
		return null;
	}

	public @Nullable T remove(int key) {
		int hash = hash(key);

		if (entries[hash] == null)
			return null;
		HashEntry<T> current_entry = entries[hash].getFirst();
		while (current_entry != null) {
			int current_key = current_entry.getKey();
			if (current_key == key) {
				T result = current_entry.getEntry();
				entries[hash].remove(current_entry);
				return result;
			}
			current_entry = current_entry.getNext();
		}
		return null;
	}

	private void rehash() {
		LinkedList<HashEntry<T>>[] old_entries = entries;
        //noinspection unchecked
        entries = (@Nullable LinkedList<@NonNull HashEntry<T>> @NonNull []) new LinkedList[entries.length*mul_factor];
        for (LinkedList<HashEntry<T>> old_entry : old_entries) {
            if (old_entry != null) {
                HashEntry<T> current_entry = old_entry.getFirst();
                while (current_entry != null) {
                    int hash = hash(current_entry.getKey());
                    HashEntry<T> next_entry = current_entry.getNext();
                    if (entries[hash] == null)
                        entries[hash] = new LinkedList<>();
                    entries[hash].addLast(current_entry);
                    current_entry = next_entry;
                }
            }
        }
	}
}
