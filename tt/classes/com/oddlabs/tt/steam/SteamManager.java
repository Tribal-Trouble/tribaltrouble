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
    private SteamAuthTicket currentAuthTicket; // Track active ticket for cancellation
    private byte[] webApiTicketData; // Cached ticket bytes from async callback
    private boolean ticketReady; // Flag indicating ticket is ready to use
    private long ticketTimestamp; // When the ticket was created (millis since epoch)

    private SteamManager() {
        if (SteamAPI.isSteamRunning()) {
            steamUser = new SteamUser(this);
            steamFriends = new SteamFriends(this);
            currentAuthTicket = null;
            webApiTicketData = null;
            ticketReady = false;
            ticketTimestamp = 0;
        } else {
            steamUser = null;
            steamFriends = null;
            currentAuthTicket = null;
            webApiTicketData = null;
            ticketReady = false;
            ticketTimestamp = 0;
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

    /**
     * Request a new Web API auth ticket (async - ticket arrives via callback).
     * Reuses cached ticket if less than 1 hour old, otherwise requests fresh ticket.
     * Call this before connecting to ensure ticket is ready.
     */
    public void requestWebApiTicket() {
        if (steamUser != null) {
            try {
                // Check if we have a fresh ticket (less than 1 hour old)
                long ticketAgeMs = System.currentTimeMillis() - ticketTimestamp;
                long oneHourMs = 60 * 60 * 1000;

                if (ticketReady && ticketAgeMs < oneHourMs) {
                    System.out.println("Steam Web API ticket still fresh (" + (ticketAgeMs / 1000) + "s old), reusing");
                    return;
                }

                // If ticket is stale, cancel it before requesting new one
                if (ticketReady && ticketAgeMs >= oneHourMs) {
                    System.out.println("Steam Web API ticket expired (" + (ticketAgeMs / 1000) + "s old), requesting fresh ticket");
                    if (currentAuthTicket != null) {
                        try {
                            steamUser.cancelAuthTicket(currentAuthTicket);
                        } catch (Exception e) {
                            System.err.println("Failed to cancel expired ticket: " + e.getMessage());
                        }
                    }
                }

                // If a request is already pending, don't start another
                if (currentAuthTicket != null && !ticketReady) {
                    System.out.println("Steam Web API ticket request already pending, waiting for callback...");
                    return;
                }

                // Mark ticket as not ready and clear old data
                ticketReady = false;
                webApiTicketData = null;
                currentAuthTicket = null;

                // Request new ticket - bytes will arrive in onGetTicketForWebApi callback
                // Use service identity for better security and auditing
                System.out.println("Requesting Steam Web API ticket...");
                System.out.println("SteamUser instance: " + steamUser);

                currentAuthTicket = steamUser.getAuthTicketForWebApi("tribaltrouble.org");

                System.out.println("getAuthTicketForWebApi() returned: " +
                    (currentAuthTicket != null ? "SUCCESS (ticket object created)" : "null"));
            } catch (NoSuchMethodError e) {
                System.err.println("ERROR: getAuthTicketForWebApi() not available in this steamworks4j version!");
                System.err.println("You may need to upgrade steamworks4j to a newer version.");
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("ERROR requesting Steam Web API ticket: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("ERROR: steamUser is null, cannot request ticket");
        }
    }

    /**
     * Get the Web API ticket bytes. Returns null if ticket is not ready yet.
     */
    public byte[] getWebApiTicket() {
        return ticketReady ? webApiTicketData : null;
    }

    /**
     * Check if a Web API ticket is ready to use.
     */
    public boolean isWebApiTicketReady() {
        return ticketReady;
    }

    /**
     * Cancel the current auth ticket if one exists.
     * Should be called when disconnecting from the server or before generating a new ticket.
     */
    public void cancelCurrentAuthTicket() {
        if (SteamAPI.isSteamRunning() && steamUser != null && currentAuthTicket != null) {
            try {
                steamUser.cancelAuthTicket(currentAuthTicket);
                System.out.println("Cancelled Steam auth ticket: " + currentAuthTicket);
            } catch (Exception e) {
                System.err.println("Failed to cancel Steam auth ticket: " + e.getMessage());
            } finally {
                currentAuthTicket = null;
            }
        }
        // Clear cached ticket data
        webApiTicketData = null;
        ticketReady = false;
        ticketTimestamp = 0;
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

    @Override
    public void onGetTicketForWebApi(
            SteamAuthTicket authTicket,
            com.codedisaster.steamworks.SteamResult result,
            byte[] ticketData) {
        System.out.println("Steam Web API ticket callback received - Result: " + result);

        if (result == com.codedisaster.steamworks.SteamResult.OK && ticketData != null && ticketData.length > 0) {
            // IMPORTANT: Must copy the bytes - cannot keep reference to ticketData
            webApiTicketData = new byte[ticketData.length];
            System.arraycopy(ticketData, 0, webApiTicketData, 0, ticketData.length);
            ticketReady = true;
            ticketTimestamp = System.currentTimeMillis();
            System.out.println("Steam Web API ticket ready: " + ticketData.length + " bytes");
        } else {
            webApiTicketData = null;
            ticketReady = false;
            ticketTimestamp = 0;
            System.err.println("Failed to get Web API ticket: " + result);
        }
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
