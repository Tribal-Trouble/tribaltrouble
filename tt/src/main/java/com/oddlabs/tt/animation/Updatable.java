package com.oddlabs.tt.animation;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface Updatable<T> {
    void update(@NonNull T anim);
}
