package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface TabListener extends EventListener {
	public void tabPressed(String[] words);
}
