package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class ConnectionInfo {
	private final int priority;
	private final List<ARMIEvent> backlog = new ArrayList<>();

	public ConnectionInfo(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public void addEvent(ARMIEvent event) {
		backlog.add(event);
	}

	public @NonNull List<ARMIEvent> getBackLog() {
		return backlog;
	}
}
