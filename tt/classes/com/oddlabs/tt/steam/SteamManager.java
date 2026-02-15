package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;

import java.nio.ByteBuffer;

/**
 * Central manager for Steam API access.
 * Provides access to Steam services like User, Friends, and Apps.
 */
public class SteamManager implements SteamUserCallback, SteamFriendsCallback {
    private static final SteamManager instance = new SteamManager();

    public static SteamManager getInstance() {
        return instance;
    }

    private SteamUser steamUser;
    private SteamFriends steamFriends;

    private SteamManager() {
        if (SteamAPI.isSteamRunning()) {
            steamUser = new SteamUser(this);
            steamFriends = new SteamFriends(this);
        } else {
            steamUser = null;
            steamFriends = null;
        }
    }

    public boolean isSteamRunning() {
        return SteamAPI.isSteamRunning();
    }

    public long getAccountID() {
        if (steamUser != null) {
            return steamUser.getSteamID().getAccountID();
        }
        return 0;
    }

    public String getPersonaName() {
        if (steamFriends != null) {
            return steamFriends.getPersonaName();
        }
        return "Unknown";
    }

    public byte[] getAuthSessionTicket() {
        if (steamUser != null) {
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
                System.err.println("Failed to get Steam auth session ticket: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    // SteamUserCallback implementation (required by SteamUser)
    @Override
    public void onValidateAuthTicket(
            SteamID steamID,
            com.codedisaster.steamworks.SteamAuth.AuthSessionResponse authSessionResponse,
            SteamID ownerSteamID) {
        // No-op for now
    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
        // No-op for now
    }

    // SteamFriendsCallback implementation (required by SteamFriends)
    @Override
    public void onPersonaStateChange(SteamID steamID,
            com.codedisaster.steamworks.SteamFriends.PersonaChange change) {
        // No-op for now
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
        // No-op for now
    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
        // No-op for now
    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
        // No-op for now
    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
        // No-op for now
    }

    @Override
    public void onGameServerChangeRequested(String server, String password) {
        // No-op for now
    }
}
