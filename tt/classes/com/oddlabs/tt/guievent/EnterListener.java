package com.oddlabs.tt.guievent;

@FunctionalInterface
public interface EnterListener extends EventListener {
	void enterPressed(CharSequence text);
}
