package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

public final class GameNetwork {
    private final Server server;
    private final @NonNull Client client;

    public GameNetwork(Server server, @NonNull Client client) {
        this.server = server;
        this.client = client;
        assert client != null;
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
