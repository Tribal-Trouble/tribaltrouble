package com.oddlabs.tt.render;

import com.oddlabs.tt.util.BoundingBox;

public final class SpriteKey extends RenderQueueKey {
	private final BoundingBox[] bounds;
	private final int[] anim_types;

	SpriteKey(int key, BoundingBox[] bounds, int[] anim_types) {
		super(key);
		this.bounds = bounds;
		this.anim_types = anim_types;
	}

	public BoundingBox getBounds(int anim_index) {
		return bounds[anim_index];
	}

	public int getAnimationType(int anim) {
		return anim_types[anim];
	}
}
