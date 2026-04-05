package com.oddlabs.matchserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public final class ServerConfiguration {
    private static final Logger logger = Logger.getLogger(ServerConfiguration.class.getName());

    public static final String DB_CONNECTION = "DB_CONNECTION";
    public static final String DB_USER = "DB_USER";
    public static final String SQL_PASS = "SQL_PASS";

    private static ServerConfiguration instance;

    public static ServerConfiguration getInstance() {
        if (instance == null) {
            instance = new ServerConfiguration("server.properties");
        }
        return instance;
    }

    private final Properties properties = new Properties();

    public ServerConfiguration(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            logger.severe("Configuration file not found: " + configFile.getAbsolutePath());
            logger.severe("Edit server.properties with your settings and restart.");
            System.exit(1);
        }
        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
            logger.info("Loaded configuration from " + configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.severe("Failed to read configuration from " + configFile.getAbsolutePath());
            System.exit(1);
        }
    }

    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        return substituteEnvVars(value);
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Support {{ENV_VAR}} syntax for environment variable substitution.
     */
    private String substituteEnvVars(String value) {
        if (value == null || !value.contains("{{")) {
            return value;
        }
        String envVarName = value.replace("{{", "").replace("}}", "").trim();
        String envValue = System.getenv(envVarName);
        return (envValue != null) ? envValue : value;
    }

}
