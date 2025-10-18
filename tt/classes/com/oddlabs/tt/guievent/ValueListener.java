package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface ValueListener extends EventListener {
	public void valueSet(int value);
}
