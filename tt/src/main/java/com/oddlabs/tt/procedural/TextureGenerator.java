package com.oddlabs.tt.procedural;

import com.oddlabs.tt.render.Texture;

import java.util.function.Supplier;

public abstract class TextureGenerator implements Supplier<Texture[]> {
	protected abstract Texture[] generate();

	@Override
	public final Texture[] get() {
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
