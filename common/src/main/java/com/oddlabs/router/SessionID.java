package com.oddlabs.router;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class SessionID implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private final long session_id;

    public SessionID(long session_id) {
        this.session_id = session_id;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof SessionID sid && sid.session_id == session_id;
    }

    @Override
    public int hashCode() {
        return (int) session_id;
    }

    @Override
    public @NonNull String toString() {
        return "(SessionID: session_id = " + session_id + ")";
    }
}
