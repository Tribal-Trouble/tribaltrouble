package com.oddlabs.tt.viewer;

public final class Cheat {
	private final boolean can_enable;
	private boolean enabled = false;
	public boolean draw_trees = true;
	public boolean line_mode = false;

	public Cheat() {
		this(Boolean.getBoolean("com.oddlabs.tt.developer"));
	}

	Cheat(boolean can_enable) {
		this.can_enable = can_enable;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void enable() {
		if (can_enable)
			enabled = true;
	}
}
