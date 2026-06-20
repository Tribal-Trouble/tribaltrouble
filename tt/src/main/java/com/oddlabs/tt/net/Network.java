package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Network {
    private final ChatHub chat_hub = new ChatHub();
    private final MatchmakingClient matchmaking_client = new MatchmakingClient(this);
    private @Nullable MatchmakingListener matchmaking_listener;

    public @Nullable MatchmakingListener getMatchmakingListener() {
        return matchmaking_listener;
    }

    public void setMatchmakingListener(@Nullable MatchmakingListener listener) {
        matchmaking_listener = listener;
    }

    public @NonNull ChatHub getChatHub() {
        return chat_hub;
    }

    public @NonNull MatchmakingClient getMatchmakingClient() {
        return matchmaking_client;
    }

    public void closeMatchmakingClient() {
        matchmaking_listener = null;
        matchmaking_client.close();
    }

    public Network() {
    }
}
