package com.oddlabs.tt.net;

import com.oddlabs.util.HashTable;

import java.util.HashMap;

public final class DistributableTable {
	private final HashTable<Distributable> distributables = new HashTable<>();
	private final HashMap<Distributable, Integer> names = new HashMap<>();
	private int current_name = 1;

	public int register(Distributable distributable) {
		int name = current_name++;
		Object o = distributables.put(name, distributable);
		assert o == null: "Error registering distributable.";
		Object p = names.put(distributable, name);
		assert p == null: "Error registering name.";
		return name;
	}

	public void unregister(Distributable distributable) {
		Integer name = names.remove(distributable);
		assert name != null: "Error unregistering name.";

		Object o = distributables.remove(name);
		assert o == distributable: "Error unregistering distributable.";
	}

	public int getName(Distributable distributable) {
		Integer val = names.get(distributable);
		assert val != null : distributable + " is not registrered.";
		return val;
	}

	public Distributable getDistributable(int name) {
		return distributables.get(name);
	}
}
