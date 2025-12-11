package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Tree {
	private final @NonNull SpriteList crown;
	private final @NonNull SpriteList trunk;

	public Tree(@NonNull SpriteList trunk, @NonNull SpriteList crown) {
		this.trunk = trunk;
		this.crown = crown;
	}

	public @NonNull SpriteList getTrunk() {
		return trunk;
	}

	public @NonNull SpriteList getCrown() {
		return crown;
	}

    @Override
    public int hashCode() {
        return crown.hashCode() + trunk.hashCode();
    }

    @Override
	public boolean equals(@Nullable Object other) {
        return other instanceof Tree other_tree && crown == other_tree.crown && trunk == other_tree.trunk;
    }
}
