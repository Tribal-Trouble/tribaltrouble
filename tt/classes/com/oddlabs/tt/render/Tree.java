package com.oddlabs.tt.render;

final class Tree {
	private final SpriteList crown;
	private final SpriteList trunk;

	public Tree(SpriteList trunk, SpriteList crown) {
		this.trunk = trunk;
		this.crown = crown;
	}

	public SpriteList getTrunk() {
		return trunk;
	}

	public SpriteList getCrown() {
		return crown;
	}

        @Override
	public boolean equals(Object other) {
		if (!(other instanceof Tree))
			return false;
		Tree other_tree = (Tree)other;
		return crown == other_tree.crown && trunk == other_tree.trunk;
	}
}
