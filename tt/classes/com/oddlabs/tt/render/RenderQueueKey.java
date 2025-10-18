package com.oddlabs.tt.render;

abstract class RenderQueueKey {
	private final int key;

	protected RenderQueueKey(int key) {
		this.key = key;
	}

	final int getKey() {
		return key;
	}
}
