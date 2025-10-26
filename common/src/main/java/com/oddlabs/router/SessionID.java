package com.oddlabs.router;

import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public final class SessionID implements Serializable {
	private final static long serialVersionUID = 1;

	private final long session_id;

	public SessionID(long session_id) {
		this.session_id = session_id;
	}

        @Override
	public boolean equals(Object other) {
		return other instanceof SessionID && ((SessionID)other).session_id == session_id;
	}

        @Override
	public int hashCode() {
		return (int)session_id;
	}

        @Override
	public @NonNull String toString() {
		return "(SessionID: session_id = " + session_id + ")";
	}
}
