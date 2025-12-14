package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamAuth.AuthSessionResponse;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import com.codedisaster.steamworks.SteamUserStats;
import com.codedisaster.steamworks.SteamUserStatsCallback;

import java.util.HashSet;
import java.util.Set;

public class SteamAchievementManager implements SteamUserStatsCallback, SteamUserCallback {
    private static final SteamAchievementManager instance = new SteamAchievementManager();

    public static SteamAchievementManager getAchievementManager() {
        return instance;
    }

    // Ensure Steam API is initialized before using the achievement manager
    static {
        SteamAchievementManager.getAchievementManager();
    }

    private SteamUserStats steamUserStats;
    private SteamUser steamUser;

    private final Set<String> unlockedAchievements = new HashSet<>();

    private boolean debugEnabled = true; // Control debug printing

    public static void debugPrint(String message) {
        if (instance != null && instance.debugEnabled) {
            System.out.println(message);
        }
    }

    public SteamAchievementManager() {
        this(false);
    }

    public SteamAchievementManager(boolean debug) {
        this.debugEnabled = debug;
        if (debug) SteamAPI.printDebugInfo(System.out);

        steamUserStats = new SteamUserStats(this);
        steamUser = new SteamUser(this);
        if (steamUserStats.requestCurrentStats()) {
            debugPrint("Successfully requested current stats.");
        } else {
            debugPrint("Failed to request current stats.");
        }
    }

    public long getAccountID() {
        return steamUser.getSteamID().getAccountID();
    }

    public int getStat(String stat_name, int default_value) {
        return steamUserStats.getStatI(stat_name, default_value);
    }

    public void setStat(String stat_name, int value) {
        steamUserStats.setStatI(stat_name, value);
        steamUserStats.storeStats();
    }

    public boolean isAchievementUnlocked(String achivement_name) {
        return steamUserStats.isAchieved(achivement_name, false);
    }

    public void updateAchievementProgress(
            String achievementId, int currentProgress, int maxProgress) {
        if (steamUserStats.indicateAchievementProgress(
                achievementId, currentProgress, maxProgress)) {
            debugPrint(
                    "Achievement progress updated: "
                            + achievementId
                            + " ("
                            + currentProgress
                            + "/"
                            + maxProgress
                            + ")");
            steamUserStats.storeStats();
        } else {
            debugPrint("Failed to update achievement progress: " + achievementId);
        }
    }

    public void unlockAchievement(String achievementId) {
        if (isAchievementUnlocked(achievementId)) {
            // debugPrint("Achievement already unlocked in this session: " + achievementId);
            return;
        }

        if (steamUserStats.setAchievement(achievementId)) {
            debugPrint("Achievement unlocked: " + achievementId);
            steamUserStats.storeStats();
            unlockedAchievements.add(achievementId);
        } else {
            debugPrint("Failed to unlock achievement: " + achievementId);
        }
    }

    public void onUserStatsStored(long gameId, int result) {
        if (result == SteamResult.OK.ordinal()) {
            debugPrint("User stats successfully stored.");
        } else {
            debugPrint("Failed to store user stats. Result code: " + result);
        }
    }

    public void onUserAchievementStored(
            long gameId,
            boolean groupAchievement,
            String achievementName,
            int curProgress,
            int maxProgress) {
        debugPrint("Achievement stored: " + achievementName);
    }

    public void onUserStatsUnloaded(SteamID steamIDUser) {
        debugPrint("User stats unloaded.");
    }

    public void onLeaderboardFindResult(SteamLeaderboardHandle leaderboard, boolean found) {
        debugPrint("Leaderboard find result: " + (found ? "Found" : "Not Found"));
    }

    public void onLeaderboardScoresDownloaded(
            SteamLeaderboardHandle leaderboard,
            SteamLeaderboardEntriesHandle entries,
            int numEntries) {
        debugPrint("Leaderboard scores downloaded: " + numEntries + " entries.");
    }

    public void onLeaderboardScoreUploaded(
            boolean success,
            SteamLeaderboardHandle leaderboard,
            int score,
            boolean scoreChanged,
            int globalRankNew,
            int globalRankPrevious) {
        debugPrint("Leaderboard score uploaded: " + (success ? "Success" : "Failed"));
    }

    public void onNumberOfCurrentPlayersReceived(boolean success, int players) {
        debugPrint("Number of current players received: " + (success ? players : "Failed"));
    }

    public void onGlobalStatsReceived(long gameId, SteamResult result) {
        debugPrint("Global stats received: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    public void onUserStatsReceived(long gameId, SteamID steamIDUser, SteamResult result) {
        debugPrint("User stats received: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    public void onUserStatsStored(long gameId, SteamResult result) {
        debugPrint("User stats stored: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    @Override
    public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {
        debugPrint(
                "Auth session ticket result: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    @Override
    public void onValidateAuthTicket(
            SteamID steamID, AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
        debugPrint(
                "Validate auth ticket result: "
                        + (authSessionResponse == AuthSessionResponse.OK ? "Success" : "Failed"));
    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
        debugPrint(
                "Micro transaction authorization result: " + (authorized ? "Success" : "Failed"));
    }

    @Override
    public void onEncryptedAppTicket(SteamResult result) {
        debugPrint(
                "Encrypted app ticket result: "
                        + (result == SteamResult.OK ? "Success" : "Failed"));
    }
}
