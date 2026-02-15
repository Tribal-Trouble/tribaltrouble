package com.oddlabs.matchserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates Steam auth tickets using the Steam Web API.
 */
public class SteamAuthValidator {
    private static final Logger logger = MatchmakingServer.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STEAM_API_URL = "https://partner.steam-api.com/ISteamUserAuth/AuthenticateUserTicket/v1/";

    /**
     * Validates a Steam auth ticket by calling the Steam Web API.
     *
     * @param steamAccountId The Steam account ID provided by the client
     * @param authTicket The auth ticket bytes
     * @return true if the ticket is valid and matches the account ID, false otherwise
     */
    public static boolean validateTicket(long steamAccountId, byte[] authTicket) {
        logger.info("=== Steam Ticket Validation Starting ===");
        logger.log(Level.INFO, "Steam Account ID: {0}", steamAccountId);

        if (authTicket == null || authTicket.length == 0) {
            logger.warning("Steam auth ticket is null or empty");
            return false;
        }

        logger.log(Level.INFO, "Auth ticket length: {0} bytes", authTicket.length);

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
            // Convert ticket bytes to hex string
            String ticketHex = bytesToHex(authTicket);

            // Build API request URL
            String urlString = STEAM_API_URL
                    + "?key=" + URLEncoder.encode(apiKey, "UTF-8")
                    + "&appid=" + URLEncoder.encode(appId, "UTF-8")
                    + "&ticket=" + URLEncoder.encode(ticketHex, "UTF-8");

            logger.log(Level.INFO, "Calling Steam API: {0}", STEAM_API_URL);
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            logger.log(Level.INFO, "Steam API response code: {0}", responseCode);

            if (responseCode != 200) {
                logger.log(Level.WARNING, "Steam API returned non-200 response: {0}", responseCode);
                return false;
            }

            // Parse JSON response into POJOs
            SteamApiAuthTicketResponse apiResponse = objectMapper.readValue(conn.getInputStream(), SteamApiAuthTicketResponse.class);
            logger.log(Level.INFO, "Steam API response: {0}", objectMapper.writeValueAsString(apiResponse));

            // Validate response structure
            if (apiResponse.response == null || apiResponse.response.params == null) {
                logger.log(Level.WARNING, "Missing response or params in Steam API response");
                return false;
            }

            SteamApiAuthTicketParams params = apiResponse.response.params;

            // Check if ticket is valid
            if (!"OK".equals(params.result)) {
                logger.log(Level.WARNING, "Steam ticket validation failed. Result: {0}", params.result);
                return false;
            }

            logger.log(Level.INFO, "Ticket validation result: {0}", params.result);

            // Validate steamid
            if (params.steamid == null || params.steamid.isEmpty()) {
                logger.log(Level.WARNING, "Missing or empty 'steamid' in Steam API response");
                return false;
            }

            logger.log(Level.INFO, "Authenticated Steam ID from API: {0}", params.steamid);

            // Convert account ID to full Steam ID (add 76561197960265728L base)
            long fullSteamId = steamAccountId + 76561197960265728L;
            String expectedSteamId = String.valueOf(fullSteamId);
            logger.log(Level.INFO, "Expected Steam ID (from account ID): {0}", expectedSteamId);

            if (!expectedSteamId.equals(params.steamid)) {
                logger.log(Level.WARNING, "Steam ID mismatch. Expected: {0}, Got: {1}", new Object[]{expectedSteamId, params.steamid});
                return false;
            }

            logger.log(Level.INFO, "Steam ID matches");
            logger.log(Level.INFO, "VALIDATION SUCCESSFUL");
            logger.log(Level.INFO, "=====================================");

            return true;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception during Steam ticket validation: {0}", e.getMessage());
            logger.throwing("SteamAuthValidator", "validateTicket", e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Converts byte array to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * POJOs for Steam API response deserialization.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamApiAuthTicketResponse {
        public SteamApiAuthTicketResponseData response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamApiAuthTicketResponseData {
        public SteamApiAuthTicketParams params;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamApiAuthTicketParams {
        public String result;
        public String steamid;
    }
}
