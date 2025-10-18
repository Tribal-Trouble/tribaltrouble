package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface MouseWheelListener extends EventListener {
	public void mouseScrolled(int amount);
}
