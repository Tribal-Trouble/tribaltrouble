package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface MouseClickListener extends EventListener {
	public void mouseClicked(int button, int x, int y, int clicks);
}
