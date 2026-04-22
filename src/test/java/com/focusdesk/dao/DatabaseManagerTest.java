package com.focusdesk.dao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

public class DatabaseManagerTest {

    @Test
    void schemaApplies() throws Exception {
        DatabaseManager.init();
    }

    @Test
    void tablesExist() throws Exception {
        DatabaseManager.init();
        try (Connection conn = DatabaseManager.getConnection()) {
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {
                while (rs.next()) System.out.println("- " + rs.getString(1));
            }
        }
    }

    @Test
    void usersTableAccessible() throws Exception {
        DatabaseManager.init();
        try (Connection conn = DatabaseManager.getConnection()) {
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
        }
    }

    @Test
    void preferencesTableAccessible() throws Exception {
        DatabaseManager.init();
        try (Connection conn = DatabaseManager.getConnection()) {
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
    }
}
