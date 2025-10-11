package com.oddlabs.matchserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class ServerConfiguration {
    public static final String SQL_PASS = "SQL_PASS";
    public static final String DISCORD_BOT_TOKEN = "DISCORD_BOT_TOKEN";
    public static final String DISCORD_SERVER_ID = "DISCORD_SERVER_ID";
    public static final String WEBSITE_DOMAIN = "WEBSITE_DOMAIN";
    public static final String VIKING_CHIEF_EMOJI = "VIKING_CHIEF_EMOJI";
    public static final String NATIVE_CHIEF_EMOJI = "NATIVE_CHIEF_EMOJI";
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
        return properties.getProperty(key);
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
                List<Map<String, String>> jsonData = mapper.readValue(
                    mappingString,
                    new TypeReference<List<Map<String, String>>>() {}
                );

                for (Map<String, String> item : jsonData) {
                    String emojiId = item.get("emoji id");
                    String roleId = item.get("role id");

                    if (emojiId != null && roleId != null) {
                        mappings.put(emojiId, roleId);
                    }
                }

            } catch (Exception e) {
                System.err.println("Error parsing emoji role mappings JSON: " + e.getMessage());
                System.err.println("Invalid JSON format: " + mappingString);
            }
        }

        return mappings;
    }
}
