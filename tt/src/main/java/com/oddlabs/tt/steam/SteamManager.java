package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SteamManager implements SteamUserCallback, SteamFriendsCallback {
    private static final Logger logger = Logger.getLogger(SteamManager.class.getName());
    private static @Nullable SteamManager instance;

    private final SteamUser steamUser;
    private final SteamFriends steamFriends;

    private SteamManager() {
        steamUser = new SteamUser(this);
        steamFriends = new SteamFriends(this);
    }

    public static @Nullable SteamManager getInstance() {
        return instance;
    }

    public static boolean init() {
        try {
            SteamAPI.loadLibraries(new SteamLibraryLoaderLwjgl3());
            if (!SteamAPI.init()) {
                logger.warning("Steam API init returned false — is the Steam client running?");
                return false;
            }
            instance = new SteamManager();
            logger.info("Steam API initialized (appId running via Steam overlay)");
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to initialize Steam API", t);
            return false;
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.steamUser.dispose();
            instance.steamFriends.dispose();
            instance = null;
        }
        SteamAPI.shutdown();
    }

    public static void runCallbacks() {
        if (instance != null) {
            SteamAPI.runCallbacks();
        }
    }

    public long getAccountID() {
        return steamUser.getSteamID().getAccountID();
    }

    public String getPersonaName() {
        return steamFriends.getPersonaName();
    }

    public byte @Nullable [] getAuthSessionTicket() {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            int[] sizeInBytes = new int[1];
            SteamAuthTicket ticket = steamUser.getAuthSessionTicket(buffer, sizeInBytes);
            if (ticket != null && sizeInBytes[0] > 0) {
                byte[] ticketData = new byte[sizeInBytes[0]];
                buffer.get(ticketData);
                return ticketData;
            }
        } catch (SteamException e) {
            logger.warning("Failed to get Steam auth session ticket: " + e.getMessage());
        }
        return null;
    }

    // SteamUserCallback
    @Override
    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
    }

    // SteamFriendsCallback
    @Override
    public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange change) {
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
    }

    @Override
    public void onGameServerChangeRequested(String server, String password) {
    }
}
