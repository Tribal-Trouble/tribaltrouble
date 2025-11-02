package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

final class Tree {
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
	public boolean equals(Object other) {
        return other instanceof Tree other_tree && crown == other_tree.crown && trunk == other_tree.trunk;
    }
}
