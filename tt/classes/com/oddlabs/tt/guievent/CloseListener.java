package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface CloseListener extends EventListener {
	public void closed();
}
