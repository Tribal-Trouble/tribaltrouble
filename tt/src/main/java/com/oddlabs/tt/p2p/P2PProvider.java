package com.oddlabs.tt.p2p;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A serverless private-match backend: session discovery (lobby, invites) plus a peer-to-peer
 * transport beneath the {@link AbstractConnection} layer. Implementations are platform SDK
 * wrappers (Steam today); everything above this interface stays provider-neutral.
 *
 * <p>All methods are main-loop-thread only, like the rest of the network stack.
 */
public interface P2PProvider {
    /** Channel for game setup negotiation (Server/Client handshake). */
    int CHANNEL_LOBBY = 0;
    /** Channel for in-game router traffic (RouterClient to the host's embedded Router). */
    int CHANNEL_GAME = 1;

    /** Callback used to hand an accepted invite to the UI layer. */
    interface JoinHandler {
        void lobbyJoined(@NonNull JoinInfo info);
    }

    /** Parameters a joiner needs before connecting, mirrored from the host's create-game dialog. */
    record JoinInfo(int gamespeed, String mapcode, float randomStartPos, int maxUnitCount, int size) {
    }

    /** Whether the provider's platform is up (e.g. the Steam client is running). */
    boolean isAvailable();

    /** The platform's display name for UI labels, e.g. "Steam". */
    @NonNull
    String getPlatformName();

    /** The local player's platform display name, used as the default game name. */
    @NonNull
    String getLocalName();

    boolean isHost();

    boolean isJoiner();

    /** Creates an invite-only session and publishes the game parameters for joiners. */
    void startHosting(@NonNull Game game);

    /** Opens the platform's invite UI for the current session (host only). */
    void openInviteDialog();

    /** Marks the match as started: the session stops accepting joins but stays alive. */
    void gameStarted();

    /** Tears down the session. Idempotent. */
    void leave();

    void setJoinHandler(@Nullable JoinHandler handler);

    /**
     * Registers a callback run when the hosted session fails to start (e.g. the platform lobby
     * could not be created). The session is already torn down when the callback runs. Cleared by
     * {@link #leave()}.
     */
    void setFailureAction(@Nullable Runnable action);

    /**
     * Scans launch arguments for a platform join request (e.g. an invite accepted while the game
     * was not running) and defers it until a join handler registers.
     */
    void handleLaunchArguments(@NonNull String @NonNull [] args);

    /** Connects to the current session's host on the given channel (joiner only). */
    @NonNull
    AbstractConnection connectToHost(int channel, @Nullable ConnectionInterface conn_interface);

    /** Listens for incoming peer connections on the given channel (host only). */
    @NonNull
    AbstractConnectionListener listen(int channel, @NonNull ConnectionListenerInterface listener_interface);

    /** Drains pending transport traffic. Called once per animation tick. */
    void pump();
}
