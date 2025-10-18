package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface EnterListener extends EventListener {
	public void enterPressed(CharSequence text);
}
