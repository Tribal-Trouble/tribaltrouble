package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface MouseClickListener extends EventListener {
	void mouseClicked(int button, int x, int y, int clicks);
}
