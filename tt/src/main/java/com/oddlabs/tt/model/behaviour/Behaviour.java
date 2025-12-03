package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

public interface Behaviour {
	enum State {
		UNINTERRUPTIBLE, INTERRUPTIBLE, DONE
	}

	@NonNull State animate(float t);
	boolean isBlocking();
	void forceInterrupted();
}
