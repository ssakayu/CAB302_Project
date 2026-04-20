package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;

import java.sql.Connection;

public class DbTest {
    public static void main(String[] args) {
        try {
            // 1) Ensure DB + tables exist (safe to run multiple times)
            DatabaseManager.init();
            System.out.println("✅ Schema applied");

            // 2) Show current DB contents
            try (Connection conn = DatabaseManager.getConnection()) {

                // List tables
                System.out.println("\n=== Tables ===");
                try (var st = conn.createStatement();
                     var rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {
                    while (rs.next()) {
                        System.out.println("- " + rs.getString(1));
                    }
                }

                // Show users
                System.out.println("\n=== Users ===");
                try (var st = conn.createStatement();
                     var rs = st.executeQuery("SELECT id, username, email, created_at FROM users ORDER BY id;")) {
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.println(
                                rs.getInt("id") + " | " +
                                        rs.getString("username") + " | " +
                                        rs.getString("email") + " | " +
                                        rs.getString("created_at")
                        );
                    }
                    if (!any) System.out.println("(no rows)");
                }

                // Show preferences
                System.out.println("\n=== Preferences ===");
                try (var st = conn.createStatement();
                     var rs = st.executeQuery("SELECT user_id, theme, enabled_widgets FROM preferences ORDER BY user_id;")) {
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.println(
                                rs.getInt("user_id") + " | " +
                                        rs.getString("theme") + " | " +
                                        rs.getString("enabled_widgets")
                        );
                    }
                    if (!any) System.out.println("(no rows)");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}