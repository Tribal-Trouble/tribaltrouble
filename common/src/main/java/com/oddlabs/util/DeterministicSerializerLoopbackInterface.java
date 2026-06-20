package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

public interface DeterministicSerializerLoopbackInterface<T> {
    default void saveSucceeded() {
    }

    default void loadSucceeded(@NonNull T object) {
    }

    void failed(@NonNull Throwable e);
}
