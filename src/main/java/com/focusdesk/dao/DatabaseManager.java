package com.focusdesk.dao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DatabaseManager {

    private static final String DB_PATH = "data/focusdesk.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private DatabaseManager() {}

    public static Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void init() throws Exception {
        // Ensure data folder exists
        Path dataDir = Path.of("data");
        if (!Files.exists(dataDir)) Files.createDirectories(dataDir);

        // Load schema.sql from resources
        String sql = loadResource("/db/schema.sql");

        // Run each statement split by ;
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String stmt : sql.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.execute(s);
            }
        }
    }

    private static String loadResource(String path) throws Exception {
        var in = DatabaseManager.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("Missing resource: " + path);

        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}