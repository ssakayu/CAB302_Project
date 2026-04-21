package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;

import java.sql.Connection;

public class DbTest {
    public static void main(String[] args) {
        try {
            DatabaseManager.init();
            System.out.println("✅ Schema applied");

            try (Connection conn = DatabaseManager.getConnection()) {

                System.out.println("\n=== Tables ===");
                try (var st = conn.createStatement();
                     var rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;")) {
                    while (rs.next()) System.out.println("- " + rs.getString(1));
                }

                System.out.println("\n=== Users ===");
                try (var st = conn.createStatement();
                     var rs = st.executeQuery("SELECT id, username, email, created_at FROM users ORDER BY id;")) {
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        System.out.println(rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getString(4));
                    }
                    if (!any) System.out.println("(no rows)");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}