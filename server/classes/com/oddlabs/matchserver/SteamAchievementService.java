package com.oddlabs.matchserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for updating player stats and achievements via Steam Web API.
 */
public final class SteamAchievementService {
    private static final Logger logger = MatchmakingServer.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STEAM_API_URL =
            "https://partner.steam-api.com/ISteamUserStats/SetUserStatsForGame/v1/";

    private SteamAchievementService() {
        // Prevent instantiation
    }

    /**
     * Updates player stats on Steam which will trigger achievement unlocks.
     *
     * @param steamId The player's Steam ID (64-bit)
     * @param totalWins Total multiplayer wins
     * @param totalLosses Total multiplayer losses
     * @param currentStreak Current win streak
     * @param bestStreak Best win streak ever achieved
     * @return true if the update was successful, false otherwise
     */
    public static boolean updatePlayerStats(
            long steamId, int totalWins, int totalLosses, int currentStreak, int bestStreak) {
        logger.info("=== Steam Stats Update Starting ===");
        logger.log(Level.INFO, "Steam ID: {0}", steamId);
        logger.log(
                Level.INFO,
                "Stats: wins={0}, losses={1}, currentStreak={2}, bestStreak={3}",
                new Object[] {totalWins, totalLosses, currentStreak, bestStreak});

        ServerConfiguration config = ServerConfiguration.getInstance();
        String apiKey = config.get(ServerConfiguration.STEAM_WEB_API_KEY);
        String appId = config.get(ServerConfiguration.STEAM_APP_ID);

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("Steam Web API key not configured");
            return false;
        }

        if (appId == null || appId.isEmpty()) {
            logger.warning("Steam App ID not configured");
            return false;
        }

        logger.log(Level.INFO, "Steam App ID: {0}", appId);
        if (apiKey.length() > 0) {
            logger.log(Level.INFO, "API Key configured: yes (length: {0})", apiKey.length());
        } else {
            logger.info("API Key configured: no");
        }

        HttpURLConnection conn = null;
        try {
            // Build POST data
            StringBuilder postData = new StringBuilder();
            postData.append("key=").append(URLEncoder.encode(apiKey, "UTF-8"));
            postData.append("&steamid=").append(steamId);
            postData.append("&appid=").append(URLEncoder.encode(appId, "UTF-8"));
            postData.append("&count=4");

            // Add stats
            postData.append("&name[0]=")
                    .append(URLEncoder.encode(SteamStatConstants.MP_TOTAL_WINS, "UTF-8"));
            postData.append("&value[0]=").append(totalWins);

            postData.append("&name[1]=")
                    .append(URLEncoder.encode(SteamStatConstants.MP_TOTAL_LOSSES, "UTF-8"));
            postData.append("&value[1]=").append(totalLosses);

            postData.append("&name[2]=")
                    .append(URLEncoder.encode(SteamStatConstants.MP_CURRENT_WIN_STREAK, "UTF-8"));
            postData.append("&value[2]=").append(currentStreak);

            postData.append("&name[3]=")
                    .append(URLEncoder.encode(SteamStatConstants.MP_BEST_WIN_STREAK, "UTF-8"));
            postData.append("&value[3]=").append(bestStreak);

            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
            logger.log(Level.INFO, "POST data length: {0} bytes", postDataBytes.length);

            // Make POST request
            logger.log(Level.INFO, "Calling Steam API: {0}", STEAM_API_URL);
            URL url = new URL(STEAM_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Write POST data
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postDataBytes);
            }

            int responseCode = conn.getResponseCode();
            logger.log(Level.INFO, "Steam API response code: {0}", responseCode);

            if (responseCode != 200) {
                logger.log(
                        Level.WARNING,
                        "Steam API returned non-200 response: {0}",
                        responseCode);
                return false;
            }

            // Parse JSON response
            SteamApiStatsResponse apiResponse =
                    objectMapper.readValue(conn.getInputStream(), SteamApiStatsResponse.class);
            logger.log(
                    Level.INFO,
                    "Steam API response: {0}",
                    objectMapper.writeValueAsString(apiResponse));

            // Validate response
            if (apiResponse.result == null) {
                logger.warning("Missing result in Steam API response");
                return false;
            }

            if (apiResponse.result.result != 1) {
                logger.log(
                        Level.WARNING,
                        "Steam stats update failed. Result code: {0}",
                        apiResponse.result.result);
                return false;
            }

            logger.info("Steam stats update result: SUCCESS");
            logger.log(
                    Level.INFO,
                    "Updated stats for steamId {0}: wins={1}, losses={2}, currentStreak={3}, bestStreak={4}",
                    new Object[] {steamId, totalWins, totalLosses, currentStreak, bestStreak});
            logger.info("=====================================");

            return true;

        } catch (Exception e) {
            logger.log(
                    Level.WARNING,
                    "Exception during Steam stats update for steamId {0}: {1}",
                    new Object[] {steamId, e.getMessage()});
            logger.throwing("SteamAchievementService", "updatePlayerStats", e);
            logger.info("STATS UPDATE FAILED");
            logger.info("=====================================");
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** POJOs for Steam API response deserialization. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamApiStatsResponse {
        public SteamApiStatsResult result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamApiStatsResult {
        public int result; // 1 = success
    }
}
