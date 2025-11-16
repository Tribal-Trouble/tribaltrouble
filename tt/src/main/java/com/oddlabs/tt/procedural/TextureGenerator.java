package com.oddlabs.tt.procedural;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

public abstract class TextureGenerator implements Supplier<@NonNull Texture @NonNull[]> {
	protected abstract @NonNull Texture @NonNull[] generate();

	@Override
	public final @NonNull Texture @NonNull[] get() {
		return generate();
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return getClass().isInstance(o);
	}
}
