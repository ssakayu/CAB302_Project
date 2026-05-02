package com.focusdesk.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Properties;

public final class AppConfig {

    private static final Path FILE = Paths.get("data/app.properties");

    private AppConfig() {}

    public static String getSpotifyClientId() {
        // 1) env var (good for dev)
        String env = System.getenv("SPOTIFY_CLIENT_ID");
        if (env != null && !env.isBlank()) return env.trim();

        // 2) local file (for real app usage)
        Properties p = load();
        return p.getProperty("spotify.clientId", "").trim();
    }

    public static void setSpotifyClientId(String clientId) {
        Properties p = load();
        p.setProperty("spotify.clientId", clientId.trim());
        save(p);
    }

    private static Properties load() {
        Properties p = new Properties();
        try {
            if (Files.exists(FILE)) {
                try (InputStream in = Files.newInputStream(FILE)) {
                    p.load(in);
                }
            }
        } catch (Exception ignored) {}
        return p;
    }

    private static void save(Properties p) {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "FocusDesk local config");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config: " + e.getMessage(), e);
        }
    }
}