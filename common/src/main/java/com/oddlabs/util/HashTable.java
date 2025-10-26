package com.oddlabs.util;

import org.jspecify.annotations.Nullable;

/**
 * Integer to object map
 *
 */
public final class HashTable<T> {
	private final static int DEFAULT_INITIAL_ENTRIES = 10;
	private final static int DEFAULT_MUL_FACTOR = 2;
	private final static float DEFAULT_LOAD_FACTOR = 0.75f;

	@SuppressWarnings("rawtypes")
	private LinkedList[] entries;
	private final float load_factor;
	private int num_entries;
	private final int mul_factor;

	public HashTable() {
		load_factor = DEFAULT_LOAD_FACTOR;
		mul_factor = DEFAULT_MUL_FACTOR;
		entries = new LinkedList[DEFAULT_INITIAL_ENTRIES];
		num_entries = 0;
	}

	public int size() {
		return num_entries;
	}

	private int hash(int key) {
		int hash = key % entries.length;
		if (hash >= 0)
			return hash;
		else
			return hash + entries.length;
	}

	public @Nullable Object put(int key, T val) {
		int hash = hash(key);
		if (entries[hash] == null) {
			entries[hash] = new LinkedList<>();
		} else {
			HashEntry<T> current_entry = (HashEntry<T>) entries[hash].getFirst();
			while (current_entry != null) {
				int current_key = current_entry.getKey();
				if (current_key == key) {
					Object result = current_entry.setEntry(val);
					return result;
				}
				current_entry = (HashEntry<T>) current_entry.getNext();
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
		HashEntry<T> current_entry = (HashEntry<T>) entries[hash].getFirst();
		while (current_entry != null) {
			int current_key = current_entry.getKey();
			if (current_key == key)
				return current_entry.getEntry();
			current_entry = (HashEntry<T>) current_entry.getNext();
		}
		return null;
	}

	public @Nullable T remove(int key) {
		int hash = hash(key);

		if (entries[hash] == null)
			return null;
		HashEntry<T> current_entry = (HashEntry<T>) entries[hash].getFirst();
		while (current_entry != null) {
			int current_key = current_entry.getKey();
			if (current_key == key) {
				T result = current_entry.getEntry();
				entries[hash].remove(current_entry);
				return result;
			}
			current_entry = (HashEntry<T>) current_entry.getNext();
		}
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void rehash() {
		LinkedList[] old_entries = entries;
		entries = new LinkedList[entries.length*mul_factor];
        for (LinkedList old_entry : old_entries) {
            if (old_entry != null) {
                HashEntry<T> current_entry = (HashEntry<T>) old_entry.getFirst();
                while (current_entry != null) {
                    int hash = hash(current_entry.getKey());
                    HashEntry<T> next_entry = (HashEntry<T>) current_entry.getNext();
                    if (entries[hash] == null)
                        entries[hash] = new LinkedList<>();
                    entries[hash].addLast(current_entry);
                    current_entry = next_entry;
                }
            }
        }
	}
}
