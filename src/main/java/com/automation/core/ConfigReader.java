package com.automation.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads config.properties. System properties (-Dkey=value) override file values.
 */
public class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {}

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return PROPS.getProperty(key, "");
    }

    public static String get(String key, String defaultValue) {
        String v = get(key);
        return v.isBlank() ? defaultValue : v;
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        return v.isBlank() ? defaultValue : Integer.parseInt(v.trim());
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String v = get(key);
        return v.isBlank() ? defaultValue : Boolean.parseBoolean(v.trim());
    }
}
