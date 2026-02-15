package com.oddlabs.matchserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfiguration {
    public static final String SQL_PASS = "SQL_PASS";
    public static final String DISCORD_BOT_TOKEN = "DISCORD_BOT_TOKEN";
    public static final String DISCORD_SERVER_ID = "DISCORD_SERVER_ID";
    public static final String WEBSITE_DOMAIN = "WEBSITE_DOMAIN";
    public static final String VIKING_CHIEF_EMOJI = "VIKING_CHIEF_EMOJI";
    public static final String NATIVE_CHIEF_EMOJI = "NATIVE_CHIEF_EMOJI";
    public static final String STEAM_WEB_API_KEY = "SteamWebAPIKey";
    public static final String STEAM_APP_ID = "SteamAppId";

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
}
