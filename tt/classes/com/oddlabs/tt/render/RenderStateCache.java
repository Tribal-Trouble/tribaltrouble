package com.oddlabs.tt.render;

import java.util.ArrayList;
import java.util.List;

final class RenderStateCache {
	private final RenderStateFactory factory;
	private final List<Object> cache = new ArrayList<>();
	private int current_index;

	RenderStateCache(RenderStateFactory factory) {
		this.factory = factory;
	}

	void clear() {
		current_index = 0;
	}

	Object get() {
		if (current_index == cache.size()) {
			cache.add(factory.create());
		}
		return cache.get(current_index++);
	}
}
