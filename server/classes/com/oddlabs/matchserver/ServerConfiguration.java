package com.oddlabs.matchserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import discord4j.core.object.reaction.ReactionEmoji;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ServerConfiguration {
    public static final String SQL_PASS = "SQL_PASS";
    public static final String DISCORD_BOT_TOKEN = "DISCORD_BOT_TOKEN";
    public static final String DISCORD_SERVER_ID = "DISCORD_SERVER_ID";
    public static final String WEBSITE_DOMAIN = "WEBSITE_DOMAIN";
    public static final String VIKING_CHIEF_EMOJI = "VIKING_CHIEF_EMOJI";
    public static final String NATIVE_CHIEF_EMOJI = "NATIVE_CHIEF_EMOJI";
    public static final String STEAM_WEB_API_KEY = "STEAM_WEB_API_KEY";
    public static final String STEAM_APP_ID = "STEAM_APP_ID";
    public static final String STEAM_ONLY_AUTH = "STEAM_ONLY_AUTH";
    /*
     * Key for emoji role mappings
     * Example JSON format for EMOJI_ROLE_MAPPINGS:
     * [{"role id":"<numeric discord role id>","emoji id":"<custom emoji id or unicode (U+1F602)>"}]
     */
    public static final String EMOJI_ROLE_MAPPINGS = "EMOJI_ROLE_MAPPINGS";
    public static final String REACTION_ROLE_MESSAGE_ID = "REACTION_ROLE_MESSAGE_ID";

    private static ServerConfiguration instance;

    public static ServerConfiguration getInstance() {
        if (instance == null) {
            instance = new ServerConfiguration("server.properties");
        }
        return instance;
    }

    private final Properties properties = new Properties();

    public ServerConfiguration(String configFilePath) {
        try (FileInputStream in = new FileInputStream(configFilePath)) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load configuration from " + configFilePath);
        }
    }

    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        // Support {{ENV_VAR}} syntax for environment variable substitution
        return substituteEnvVars(value);
    }

    private String substituteEnvVars(String value) {
        if (value == null || !value.contains("{{")) {
            return value;
        }
        // Simple substitution: {{VAR_NAME}} -> System.getenv("VAR_NAME")
        String envVarName = value.replace("{{", "").replace("}}", "").trim();
        String envValue = System.getenv(envVarName);
        return (envValue != null) ? envValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public Map<String, String> getEmojiRoleMappings() {
        Map<String, String> mappings = new HashMap<>();
        String mappingString = get(EMOJI_ROLE_MAPPINGS);

        if (mappingString != null && !mappingString.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, String>> jsonData =
                        mapper.readValue(
                                mappingString, new TypeReference<List<Map<String, String>>>() {});

                for (Map<String, String> item : jsonData) {
                    String emojiId = item.get("emoji id");
                    emojiId = normalizeEmojiKey(emojiId);
                    if (emojiId == null) continue;
                    String roleId = item.get("role id");

                    if (roleId != null) {
                        mappings.put(emojiId, roleId);
                    }
                }

            } catch (Exception e) {
                System.out.println("Error parsing emoji role mappings JSON: " + e.getMessage());
                System.out.println("Invalid JSON format: " + mappingString);
            }
        }

        return mappings;
    }

    /* Normalizes the emoji key if a custom emoji then the custom long id is used.
       If a unicode emoji is provided, it is used to construct a ReactionEmoji from it then getRaw()
       so when a reaction is posted to the bot from discord it can find the unicode key easily.
       Unicode values can be found at: https://unicode.org/emoji/charts/full-emoji-list.html
    */
    private String normalizeEmojiKey(String emojiId) {
        long val = -1;
        try {
            val = Long.parseLong(emojiId);
        } catch (NumberFormatException e) {
            // If unparsable it should be interpretted as unicode character
            if (!emojiId.startsWith("U+")) {
                System.out.println(
                        "The argument(s) to this method should use the \"U+\" notation for"
                                + " codepoints. Skipping mapping: "
                                + emojiId);
                return null;
            }
        }

        if (val == -1) {
            System.out.println("Interpreting emoji id as codepoint: " + emojiId);
            emojiId = ReactionEmoji.codepoints(emojiId).getRaw();
        }
        return emojiId;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    public boolean isSteamOnlyAuth() {
        return getBoolean(STEAM_ONLY_AUTH, false);
    }
}
