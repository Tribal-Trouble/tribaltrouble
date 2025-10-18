package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface TabListener extends EventListener {
	void tabPressed(String[] words);
}
