package com.oddlabs.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class HostSequenceID implements Serializable {
    @Serial
    private static final long serialVersionUID = -8670168662221748395L;

    private final int host_id;
    private final int seq_id;

    public HostSequenceID(int host_id, int seq_id) {
        this.host_id = host_id;
        this.seq_id = seq_id;
    }

    public int getHostID() {
        return host_id;
    }

    public int getSequenceID() {
        return seq_id;
    }

    @Override
    public int hashCode() {
        return host_id ^ seq_id;
    }

    @Override
    public @NonNull String toString() {
        return host_id + " " + seq_id;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof HostSequenceID other_id &&
                host_id == other_id.host_id &&
                seq_id == other_id.seq_id;
    }
}
