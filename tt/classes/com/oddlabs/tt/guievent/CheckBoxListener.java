package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface CheckBoxListener extends EventListener {
	public void checked(boolean marked);
}
