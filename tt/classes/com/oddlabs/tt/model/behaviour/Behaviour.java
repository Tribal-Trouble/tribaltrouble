package com.oddlabs.tt.model.behaviour;

public interface Behaviour {
	int animate(float t);
	boolean isBlocking();
	void forceInterrupted();
}
