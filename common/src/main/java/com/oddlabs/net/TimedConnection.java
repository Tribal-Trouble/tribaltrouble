package com.oddlabs.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class TimedConnection {
    private final long timeout;
    private final @NonNull Connection conn;

    public TimedConnection(long timeout, @NonNull Connection conn) {
        this.timeout = timeout;
        this.conn = conn;
    }

    public long getTimeout() {
        return timeout;
    }

    public @NonNull Connection getConnection() {
        return conn;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TimedConnection other_timed &&
                other_timed.conn.equals(this.conn);
    }

    @Override
    public int hashCode() {
        return conn.hashCode();
    }
}
