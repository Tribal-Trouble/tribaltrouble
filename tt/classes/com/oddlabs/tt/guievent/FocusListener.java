package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface FocusListener extends EventListener {
	public void activated(boolean activated);
}
