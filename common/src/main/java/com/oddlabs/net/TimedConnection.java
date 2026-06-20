package com.oddlabs.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record TimedConnection(long timeout, @NonNull Connection connection) {
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TimedConnection other_timed &&
                other_timed.connection.equals(this.connection);
    }

    @Override
    public int hashCode() {
        return connection.hashCode();
    }
}
