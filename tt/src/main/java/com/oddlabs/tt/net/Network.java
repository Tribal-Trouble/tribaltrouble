package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Network {
    private static final ChatHub chat_hub = new ChatHub();
    private static final MatchmakingClient matchmaking_client = new MatchmakingClient();
    private static @Nullable MatchmakingListener matchmaking_listener;

    public static @Nullable MatchmakingListener getMatchmakingListener() {
        return matchmaking_listener;
    }

    public static void setMatchmakingListener(MatchmakingListener listener) {
        matchmaking_listener = listener;
    }

    public static void closeMatchmakingClient() {
        matchmaking_listener = null;
        matchmaking_client.close();
    }

    public static @NonNull ChatHub getChatHub() {
        return chat_hub;
    }

    public static @NonNull MatchmakingClient getMatchmakingClient() {
        return matchmaking_client;
    }

    private Network() {
    }
}
