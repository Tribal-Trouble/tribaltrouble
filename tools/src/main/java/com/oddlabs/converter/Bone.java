package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

public final class Bone {
	private final @NonNull String name;
	private final byte index;
	private final @NonNull Bone @NonNull[] children;

	public Bone(@NonNull String name, byte index, @NonNull Bone @NonNull[] children) {
		this.name = name;
		this.children = children;
		this.index = index;
	}

	public @NonNull Bone @NonNull[] getChildren() {
		return children;
	}

	public byte getIndex() {
		return index;
	}

	public @NonNull String getName() {
		return name;
	}
}

