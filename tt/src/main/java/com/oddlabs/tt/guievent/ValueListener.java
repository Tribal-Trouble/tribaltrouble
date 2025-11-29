package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface ValueListener extends EventListener {
	void valueSet(long value);
}
