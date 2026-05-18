package com.oddlabs.matchserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oddlabs.matchserver.models.SteamStatsResponse;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for updating player stats and achievements via Steam Web API.
 */
public final class SteamAchievementService {
    private static final Logger logger = MatchmakingServer.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STEAM_API_URL = "https://partner.steam-api.com/ISteamUserStats/SetUserStatsForGame/v1/";

    private SteamAchievementService() {
    }

    /**
     * Updates player stats on Steam which will trigger achievement unlocks.
     *
     * @param steamId       The player's Steam ID (64-bit)
     * @param totalWins     Total multiplayer wins
     * @param totalLosses   Total multiplayer losses
     * @param currentStreak Current win streak
     * @param bestStreak    Best win streak ever achieved
     * @return true if the update was successful, false otherwise
     */
    public static boolean updatePlayerStats(
            long steamId, int totalWins, int totalLosses, int currentStreak, int bestStreak) {
        ServerConfiguration config = ServerConfiguration.getInstance();
        String apiKey = config.get(ServerConfiguration.STEAM_WEB_API_KEY);
        int appId = config.getMainSteamAppId();

        if (apiKey == null || apiKey.isEmpty() || appId == -1) {
            logger.warning("Steam Web API key or App ID not configured");
            return false;
        }

        HttpURLConnection conn = null;
        try {
            StringBuilder postData = new StringBuilder();
            postData.append("key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            postData.append("&steamid=").append(steamId);
            postData.append("&appid=").append(appId);
            postData.append("&count=4");

            postData.append("&name[0]=").append(URLEncoder.encode(SteamStatConstants.MP_TOTAL_WINS,
                    StandardCharsets.UTF_8));
            postData.append("&value[0]=").append(totalWins);

            postData.append("&name[1]=").append(URLEncoder.encode(SteamStatConstants.MP_TOTAL_LOSSES,
                    StandardCharsets.UTF_8));
            postData.append("&value[1]=").append(totalLosses);

            postData.append("&name[2]=").append(URLEncoder.encode(SteamStatConstants.MP_CURRENT_WIN_STREAK,
                    StandardCharsets.UTF_8));
            postData.append("&value[2]=").append(currentStreak);

            postData.append("&name[3]=").append(URLEncoder.encode(SteamStatConstants.MP_BEST_WIN_STREAK,
                    StandardCharsets.UTF_8));
            postData.append("&value[3]=").append(bestStreak);

            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) URI.create(STEAM_API_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postDataBytes);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.log(Level.WARNING, "Steam stats API returned {0}", responseCode);
                return false;
            }

            SteamStatsResponse statsResponse = objectMapper.readValue(conn.getInputStream(), SteamStatsResponse.class);

            if (statsResponse.result == null || statsResponse.result.result != 1) {
                logger.log(Level.WARNING, "Steam stats update failed. Result: {0}",
                        statsResponse.result != null ? statsResponse.result.result : "null");
                return false;
            }

            logger.log(Level.INFO, "Steam stats updated for {0}: wins={1}, losses={2}, streak={3}, best={4}",
                    new Object[]{steamId, totalWins, totalLosses, currentStreak, bestStreak});
            return true;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Steam stats update failed for {0}: {1}",
                    new Object[]{steamId, e.getMessage()});
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
