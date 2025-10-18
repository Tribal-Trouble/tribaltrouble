package com.oddlabs.tt.resource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Provides a cache of resources by their suppliers
 */
public final class Resources {
	private final static ConcurrentMap<Supplier<?>, Object> LOADED_RESOURCES = new ConcurrentHashMap<>();
	public static <R> R findResource(Supplier<R> resSupplier) {
        //noinspection unchecked
        return (R) LOADED_RESOURCES.computeIfAbsent(resSupplier, Supplier::get);
	}

    private Resources() {
    }
}
