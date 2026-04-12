package com.oddlabs.matchserver;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oddlabs.matchserver.models.SteamAuthResponse;

/**
 * Validates Steam auth tickets using the Steam Web API.
 */
public final class SteamAuthValidator {
    private static final Logger logger = MatchmakingServer.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String STEAM_API_URL = "https://partner.steam-api.com/ISteamUserAuth/AuthenticateUserTicket/v1/";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Steam ID 64 base offset (converts account ID to full Steam ID)
    private static final long STEAM_ID_BASE = 76561197960265728L;

    public static boolean validateTicket(long steamAccountId, byte[] authTicket) {
        if (authTicket == null || authTicket.length == 0) {
            logger.warning("Steam auth ticket is null or empty");
            return false;
        }

        ServerConfiguration config = ServerConfiguration.getInstance();
        String apiKey = config.get(ServerConfiguration.STEAM_WEB_API_KEY);
        String appId = config.get(ServerConfiguration.STEAM_APP_ID);

        if (apiKey == null || apiKey.isEmpty() || appId == null || appId.isEmpty()) {
            logger.warning("Steam Web API key or App ID not configured");
            return false;
        }

        String ticketHex = bytesToHex(authTicket);
        String url = STEAM_API_URL
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&appid=" + URLEncoder.encode(appId, StandardCharsets.UTF_8)
                + "&ticket=" + URLEncoder.encode(ticketHex, StandardCharsets.UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Steam API response: status=" + response.statusCode());

            if (response.statusCode() != 200) {
                logger.warning("Steam API returned " + response.statusCode() + ": " + response.body());
                return false;
            }

            SteamAuthResponse authResponse = objectMapper.readValue(response.body(), SteamAuthResponse.class);

            if (authResponse.response == null) {
                logger.warning("Missing response in Steam API response: " + response.body());
                return false;
            }

            if (authResponse.response.params == null) {
                if (authResponse.response.error != null) {
                    logger.warning("Steam API error " + authResponse.response.error.errorcode
                            + ": " + authResponse.response.error.errordesc);
                } else {
                    logger.warning("Missing params in Steam API response: " + response.body());
                }
                return false;
            }

            if (!"OK".equals(authResponse.response.params.result)) {
                logger.warning("Steam ticket validation result: " + authResponse.response.params.result);
                return false;
            }

            String expectedSteamId = String.valueOf(steamAccountId + STEAM_ID_BASE);
            if (!expectedSteamId.equals(authResponse.response.params.steamid)) {
                logger.warning("Steam ID mismatch: expected " + expectedSteamId + ", got "
                        + authResponse.response.params.steamid);
                return false;
            }

            logger.info("Steam ticket validated for account " + steamAccountId);
            return true;

        } catch (Exception e) {
            logger.warning("Steam ticket validation failed: " + e.getMessage());
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
