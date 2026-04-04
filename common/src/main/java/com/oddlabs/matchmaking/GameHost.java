package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class GameHost implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private final @NonNull Game game;
    private final int host_id;
    private final int revision;

    public GameHost(@NonNull Game game, int host_id, int revision) {
        this.host_id = host_id;
        this.game = game;
        this.revision = revision;
    }

    public @NonNull Game getGame() {
        return game;
    }

    public int getHostID() {
        return host_id;
    }

    public int getRevision() {
        return revision;
    }
}
