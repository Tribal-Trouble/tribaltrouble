package com.oddlabs.tt.util;

/**
 * An AutoCloseable resource that does not throw checked exceptions,
 * suitable for use in try-with-resources statements for OpenGL state management.
 */
public interface GLState extends AutoCloseable {
    @Override
    void close();
}
