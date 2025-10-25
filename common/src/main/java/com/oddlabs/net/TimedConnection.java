package com.oddlabs.net;

public final class TimedConnection {
	private final long timeout;
	private final Connection conn;

	public TimedConnection(long timeout, Connection conn) {
		this.timeout = timeout;
		this.conn = conn;
	}

	public long getTimeout() {
		return timeout;
	}

	public Connection getConnection() {
		return conn;
	}

        @Override
	public boolean equals(Object other) {
		if (!(other instanceof TimedConnection))
			return false;
		TimedConnection other_timed = (TimedConnection)other;
		return other_timed.conn.equals(this.conn);
	}

        @Override
	public int hashCode() {
		return conn.hashCode();
	}
}
