package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.RosterTemplate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class GameNetwork {
    private final Server server;
    private final @NonNull Client client;

    /**
     * Roster the host configured in the create-game dialog, applied once when the host's lobby opens. Null for joiners
     * and for the non-multiplayer path.
     */
    private @Nullable RosterTemplate initial_roster;

    public GameNetwork(Server server, @NonNull Client client) {
        this.server = server;
        this.client = client;
        assert client != null;
    }

    public void setInitialRoster(@Nullable RosterTemplate initial_roster) {
        this.initial_roster = initial_roster;
    }

    public @Nullable RosterTemplate getInitialRoster() {
        return initial_roster;
    }

    public void closeServer() {
        if (server != null)
            server.close();
    }

    public @NonNull Client getClient() {
        return client;
    }

    public void close() {
        client.close();
        closeServer();
    }
}
