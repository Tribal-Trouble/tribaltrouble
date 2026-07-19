package com.oddlabs.tt.steam;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.tt.p2p.P2PProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.logging.Logger;

/**
 * The Steam implementation of {@link P2PProvider}: friends-only Steam lobbies for discovery and
 * invites, legacy SteamNetworking reliable P2P packets for transport.
 */
public final class SteamP2PProvider implements P2PProvider {
    private static final Logger logger = Logger.getLogger(SteamP2PProvider.class.getName());

    @Override
    public boolean isAvailable() {
        return SteamManager.getInstance() != null;
    }

    @Override
    public @NonNull String getPlatformName() {
        return "Steam";
    }

    @Override
    public @NonNull String getLocalName() {
        SteamManager steam = SteamManager.getInstance();
        return steam != null ? steam.getPersonaName() : "";
    }

    @Override
    public boolean isHost() {
        return SteamLobbySession.isHost();
    }

    @Override
    public boolean isJoiner() {
        return SteamLobbySession.isJoiner();
    }

    @Override
    public void startHosting(@NonNull Game game) {
        SteamLobbySession.startHosting(game);
    }

    @Override
    public void openInviteDialog() {
        SteamLobbySession.openInviteDialog();
    }

    @Override
    public void gameStarted() {
        SteamLobbySession.gameStarted();
    }

    @Override
    public void leave() {
        SteamLobbySession.leave();
    }

    @Override
    public void setJoinHandler(@Nullable JoinHandler handler) {
        SteamLobbySession.setJoinHandler(handler);
    }

    @Override
    public void handleLaunchArguments(@NonNull String @NonNull [] args) {
        // Steam passes "+connect_lobby <id>" when the game is launched by accepting an invite
        // while it was not running.
        for (int i = 0; i < args.length - 1; i++) {
            if ("+connect_lobby".equals(args[i])) {
                try {
                    SteamLobbySession.setPendingLaunchLobby(Long.parseLong(args[i + 1]));
                } catch (NumberFormatException e) {
                    logger.warning("Malformed +connect_lobby argument: " + args[i + 1]);
                }
            }
        }
    }

    @Override
    public @NonNull AbstractConnection connectToHost(int channel, @Nullable ConnectionInterface conn_interface) {
        return new SteamP2PConnection(SteamLobbySession.getHostID(), channel, conn_interface);
    }

    @Override
    public @NonNull AbstractConnectionListener listen(int channel,
            @NonNull ConnectionListenerInterface listener_interface) {
        return new SteamP2PConnectionListener(channel, listener_interface);
    }

    @Override
    public void pump() {
        SteamP2P.pump();
    }
}
