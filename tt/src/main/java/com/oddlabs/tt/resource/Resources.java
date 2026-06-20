package com.oddlabs.tt.resource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a cache of resources by their suppliers
 */
public final class Resources {
    private static final Logger logger = Logger.getLogger(Resources.class.getSimpleName());
    // TODO Consider replacing with WeakHashMap, but some native resources may not be strongly held so would be GCed.
    private static final ConcurrentMap<@NonNull Supplier<? extends @NonNull Object>, @NonNull Object> LOADED_RESOURCES
            = new ConcurrentHashMap<>();

    public static <R> @NonNull R findResource(@NonNull Supplier<R> resSupplier) {
        Object resource = LOADED_RESOURCES.get(resSupplier);
        // We can't use computeIfAbsent because some resources are recursive
        if (resource == null) {
            resource = resSupplier.get();
            Object existing = LOADED_RESOURCES.putIfAbsent(resSupplier, resource);
            if (existing != null) {
                // There was a race we lost.
                resource = existing;
            }
        }
        //noinspection unchecked
        return (R) resource;
    }

    /**
     * Clear all loaded resources
     */
    public static void clearResources() {
        for (Object resource : LOADED_RESOURCES.values()) {
            closeResource(resource);
        }
        LOADED_RESOURCES.clear();
    }

    private static void closeResource(@Nullable Object resource) {
        if (resource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to close resource " + resource, e);
            }
        } else if (resource instanceof Object[] array) {
            for (Object obj : array) {
                closeResource(obj);
            }
        }
    }

    private Resources() {
    }
}
