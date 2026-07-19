package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmakingCallback;
import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamResult;
import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.p2p.P2PProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.logging.Logger;

/**
 * A serverless private match session backed by a Steam lobby. The lobby is only used for
 * discovery and signaling: the host publishes its SteamID and the world parameters as lobby data,
 * invited friends join the lobby, read that data, and connect directly to the host over
 * {@link SteamP2P}. No matchmaking or router server is involved.
 *
 * <p>The session is a process-wide singleton. It is created when hosting or joining starts and
 * lives until the player returns to the main menu, because the in-game transport keeps using the
 * host SteamID long after the Steam lobby itself has served its purpose.
 */
public final class SteamLobbySession implements SteamMatchmakingCallback {
    private static final String KEY_HOST = "tt_host";
    private static final String KEY_GAMESPEED = "tt_gamespeed";
    private static final String KEY_MAPCODE = "tt_mapcode";
    private static final String KEY_RANDOM_START = "tt_randstart";
    private static final String KEY_MAX_UNITS = "tt_maxunits";
    private static final String KEY_SIZE = "tt_size";

    private static final Logger logger = Logger.getLogger(SteamLobbySession.class.getName());

    private static @Nullable SteamLobbySession active;
    private static P2PProvider.@Nullable JoinHandler join_handler;
    private static long pending_launch_lobby;

    private final SteamMatchmaking matchmaking;
    private final boolean is_host;
    private @Nullable Game game; // host only, published as lobby data once the lobby exists
    private @Nullable SteamID lobby_id;
    private @Nullable SteamID host_id;

    private SteamLobbySession(boolean is_host) {
        this.matchmaking = new SteamMatchmaking(this);
        this.is_host = is_host;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static boolean isHost() {
        return active != null && active.is_host;
    }

    public static boolean isJoiner() {
        return active != null && !active.is_host;
    }

    /** The host's SteamID, valid on a joiner once the lobby has been entered. */
    public static @NonNull SteamID getHostID() {
        assert active != null && active.host_id != null;
        return active.host_id;
    }

    /**
     * Records the lobby id from a "+connect_lobby" launch argument. The join is deferred until
     * the main menu registers its join handler, since at parse time no GUI exists to receive it.
     */
    public static void setPendingLaunchLobby(long lobby_handle) {
        pending_launch_lobby = lobby_handle;
    }

    public static void setJoinHandler(P2PProvider.@Nullable JoinHandler handler) {
        join_handler = handler;
        if (handler != null && pending_launch_lobby != 0) {
            long lobby = pending_launch_lobby;
            pending_launch_lobby = 0;
            joinRequested(SteamID.createFromNativeHandle(lobby));
        }
    }

    /**
     * Starts hosting: creates a friends-only Steam lobby and publishes the game parameters as
     * lobby data once creation completes. Safe to call before the Server exists; joiners can only
     * connect after the lobby data is visible.
     */
    public static void startHosting(@NonNull Game game) {
        if (SteamManager.getInstance() == null) {
            logger.warning("Cannot host Steam game: Steam not initialized");
            return;
        }
        leave();
        SteamLobbySession session = new SteamLobbySession(true);
        session.game = game;
        session.host_id = SteamManager.getInstance().getSteamID();
        active = session;
        session.matchmaking.createLobby(SteamMatchmaking.LobbyType.FriendsOnly,
                MatchmakingServerInterface.MAX_PLAYERS);
    }

    /** Handles an accepted Steam invite or a join from the friends list. */
    public static void joinRequested(@NonNull SteamID lobby) {
        if (SteamManager.getInstance() == null)
            return;
        if (active != null) {
            // Hosting, joining or already in a lobby: tearing the live session down here would
            // pull it out from under the open lobby UI and its connections.
            logger.info("Ignoring Steam join request while a session is active");
            return;
        }
        SteamLobbySession session = new SteamLobbySession(false);
        active = session;
        session.matchmaking.joinLobby(lobby);
    }

    /** Opens the Steam overlay invite dialog for the current lobby (host only). */
    public static void openInviteDialog() {
        if (active != null && active.lobby_id != null && SteamManager.getInstance() != null)
            SteamManager.getInstance().activateInviteDialog(active.lobby_id);
    }

    /** Marks the match as started: the lobby stops accepting joins but the session stays alive. */
    public static void gameStarted() {
        if (active != null && active.is_host && active.lobby_id != null)
            active.matchmaking.setLobbyJoinable(active.lobby_id, false);
    }

    /** Tears down the session and leaves the Steam lobby. Idempotent. */
    public static void leave() {
        if (active != null) {
            if (active.lobby_id != null)
                active.matchmaking.leaveLobby(active.lobby_id);
            active.matchmaking.dispose();
            active = null;
        }
    }

    // SteamMatchmakingCallback

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
        if (active != this)
            return;
        if (result != SteamResult.OK) {
            logger.warning("Steam lobby creation failed: " + result);
            leave();
            return;
        }
        lobby_id = steamIDLobby;
        assert game != null && host_id != null;
        matchmaking.setLobbyData(steamIDLobby, KEY_HOST, Long.toString(SteamNativeHandle.getNativeHandle(host_id)));
        matchmaking.setLobbyData(steamIDLobby, KEY_GAMESPEED, Integer.toString(game.getGamespeed()));
        matchmaking.setLobbyData(steamIDLobby, KEY_MAPCODE, game.getMapcode());
        matchmaking.setLobbyData(steamIDLobby, KEY_RANDOM_START, Float.toString(game.getRandomStartPos()));
        matchmaking.setLobbyData(steamIDLobby, KEY_MAX_UNITS, Integer.toString(game.getMaxUnitCount()));
        matchmaking.setLobbyData(steamIDLobby, KEY_SIZE, Integer.toString(game.getSize()));
        logger.info("Steam lobby created: " + steamIDLobby);
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked,
            SteamMatchmaking.ChatRoomEnterResponse response) {
        if (active != this || is_host)
            return;
        if (response != SteamMatchmaking.ChatRoomEnterResponse.Success) {
            logger.warning("Failed to enter Steam lobby: " + response);
            leave();
            return;
        }
        lobby_id = steamIDLobby;
        P2PProvider.JoinInfo info;
        long host_handle;
        try {
            host_handle = Long.parseLong(matchmaking.getLobbyData(steamIDLobby, KEY_HOST));
            info = new P2PProvider.JoinInfo(
                    Integer.parseInt(matchmaking.getLobbyData(steamIDLobby, KEY_GAMESPEED)),
                    matchmaking.getLobbyData(steamIDLobby, KEY_MAPCODE),
                    Float.parseFloat(matchmaking.getLobbyData(steamIDLobby, KEY_RANDOM_START)),
                    Integer.parseInt(matchmaking.getLobbyData(steamIDLobby, KEY_MAX_UNITS)),
                    Integer.parseInt(matchmaking.getLobbyData(steamIDLobby, KEY_SIZE)));
        } catch (NumberFormatException | NullPointerException e) {
            logger.warning("Steam lobby is missing game data, leaving: " + e);
            leave();
            return;
        }
        host_id = SteamID.createFromNativeHandle(host_handle);
        logger.info("Entered Steam lobby " + steamIDLobby + ", host " + host_id);
        if (join_handler != null)
            join_handler.lobbyJoined(info);
        else
            logger.warning("No Steam join handler registered, ignoring lobby join");
    }
}
