package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserStats;
import com.codedisaster.steamworks.SteamUserStatsCallback;

import java.util.HashSet;
import java.util.Set;

public class SteamAchievementManager implements SteamUserStatsCallback {
    private static final SteamAchievementManager instance = new SteamAchievementManager();

    public static SteamAchievementManager getAchievementManager() {
        return instance;
    }

    // Ensure Steam API is initialized before using the achievement manager
    static {
        SteamAchievementManager.getAchievementManager();
    }

    private SteamUserStats steamUserStats;

    private final Set<String> unlockedAchievements = new HashSet<>();

    public SteamAchievementManager() {
        SteamAPI.printDebugInfo(System.out);
        steamUserStats = new SteamUserStats(this);
        if (steamUserStats.requestCurrentStats()) {
            System.out.println("Successfully requested current stats.");
        } else {
            System.err.println("Failed to request current stats.");
        }
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
            System.out.println(
                    "Achievement progress updated: "
                            + achievementId
                            + " ("
                            + currentProgress
                            + "/"
                            + maxProgress
                            + ")");
            steamUserStats.storeStats();
        } else {
            System.err.println("Failed to update achievement progress: " + achievementId);
        }
    }

    public void unlockAchievement(String achievementId) {
        if (isAchievementUnlocked(achievementId)) {
            // System.out.println("Achievement already unlocked in this session: " + achievementId);
            return;
        }

        if (steamUserStats.setAchievement(achievementId)) {
            System.out.println("Achievement unlocked: " + achievementId);
            steamUserStats.storeStats();
            unlockedAchievements.add(achievementId);
        } else {
            System.err.println("Failed to unlock achievement: " + achievementId);
        }
    }

    public void onUserStatsStored(long gameId, int result) {
        if (result == SteamResult.OK.ordinal()) {
            System.out.println("User stats successfully stored.");
        } else {
            System.err.println("Failed to store user stats. Result code: " + result);
        }
    }

    public void onUserAchievementStored(
            long gameId,
            boolean groupAchievement,
            String achievementName,
            int curProgress,
            int maxProgress) {
        System.out.println("Achievement stored: " + achievementName);
    }

    public void onUserStatsUnloaded(SteamID steamIDUser) {
        System.out.println("User stats unloaded.");
    }

    public void onLeaderboardFindResult(SteamLeaderboardHandle leaderboard, boolean found) {
        System.out.println("Leaderboard find result: " + (found ? "Found" : "Not Found"));
    }

    public void onLeaderboardScoresDownloaded(
            SteamLeaderboardHandle leaderboard,
            SteamLeaderboardEntriesHandle entries,
            int numEntries) {
        System.out.println("Leaderboard scores downloaded: " + numEntries + " entries.");
    }

    public void onLeaderboardScoreUploaded(
            boolean success,
            SteamLeaderboardHandle leaderboard,
            int score,
            boolean scoreChanged,
            int globalRankNew,
            int globalRankPrevious) {
        System.out.println("Leaderboard score uploaded: " + (success ? "Success" : "Failed"));
    }

    public void onNumberOfCurrentPlayersReceived(boolean success, int players) {
        System.out.println("Number of current players received: " + (success ? players : "Failed"));
    }

    public void onGlobalStatsReceived(long gameId, SteamResult result) {
        System.out.println(
                "Global stats received: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    public void onUserStatsReceived(long gameId, SteamID steamIDUser, SteamResult result) {
        System.out.println(
                "User stats received: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }

    public void onUserStatsStored(long gameId, SteamResult result) {
        System.out.println(
                "User stats stored: " + (result == SteamResult.OK ? "Success" : "Failed"));
    }
}
