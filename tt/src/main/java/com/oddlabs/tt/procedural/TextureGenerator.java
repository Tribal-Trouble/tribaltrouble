package com.oddlabs.tt.procedural;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public abstract class TextureGenerator implements Supplier<@NonNull Texture @NonNull []> {
    protected abstract @NonNull Texture @NonNull [] generate();

    @Override
    public final @NonNull Texture @NonNull [] get() {
        return generate();
    }

    @Override
    public int hashCode() {
        return getClass().getSimpleName().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return getClass().isInstance(o);
    }
}
