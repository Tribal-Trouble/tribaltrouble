package com.oddlabs.tt.resource;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Provides a cache of resources by their suppliers
 */
public final class Resources {
	private final static ConcurrentMap<Supplier<?>, Object> LOADED_RESOURCES = new ConcurrentHashMap<>();
	public static <R> R findResource(@NonNull Supplier<R> resSupplier) {
		Object resource = LOADED_RESOURCES.get(resSupplier);
		if (resource == null) {
			resource = resSupplier.get();
			Object existing = LOADED_RESOURCES.putIfAbsent(resSupplier, resource);
			if (existing != null) {
				resource = existing;
			}
		}
        //noinspection unchecked
        return (R) resource;
	}

    private Resources() {
    }
}
