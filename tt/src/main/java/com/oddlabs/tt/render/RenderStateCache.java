package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Caches render state objects for reuse.
 * @param <RS>
 */
final class RenderStateCache<RS extends LODObject> {
	private final RenderStateFactory<RS> factory;
	private final List<@NonNull RS> cache = new ArrayList<>();
	private int current_index;

	RenderStateCache(@NonNull RenderStateFactory<@NonNull RS> factory) {
		this.factory = factory;
	}

	void clear() {
		current_index = 0;
	}

	@NonNull RS get() {
		if (current_index == cache.size()) {
			cache.add(factory.create());
		}
		return cache.get(current_index++);
	}
}
