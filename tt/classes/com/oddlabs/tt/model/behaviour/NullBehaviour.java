package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Selectable;

public final class NullBehaviour implements Behaviour {
	public NullBehaviour() {
	}

    @Override
	public int animate(float t) {
		return Selectable.INTERRUPTIBLE;
	}

    @Override
	public boolean isBlocking() {
		return true;
	}

    @Override
	public void forceInterrupted() {
	}
}
