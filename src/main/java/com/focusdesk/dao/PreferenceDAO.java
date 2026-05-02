
package com.focusdesk.dao;

import com.focusdesk.model.Preference;
import com.focusdesk.model.PomodoroSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PreferenceDAO {

    public Preference getByUserId(int userId) throws Exception {
        String sql = """
            SELECT user_id, theme, enabled_widgets, focus_minutes, short_break_minutes, long_break_minutes,
                   sessions_before_long_break, enable_sound_notifications,
                   widget_x, widget_y, widget_opacity
            FROM preferences
            WHERE user_id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Preference(
                        rs.getInt("user_id"),
                        rs.getString("theme"),
                        rs.getString("enabled_widgets"),
                        rs.getInt("focus_minutes"),
                        rs.getInt("short_break_minutes"),
                        rs.getInt("long_break_minutes"),
                        rs.getInt("sessions_before_long_break"),
                        rs.getInt("enable_sound_notifications") != 0,
                        rs.getDouble("widget_x"),
                        rs.getDouble("widget_y"),
                        rs.getDouble("widget_opacity")
                );
            }
        }
    }

    public void ensureDefaultRow(int userId) throws Exception {
        // Insert defaults only if missing
        String sql = """
            INSERT OR IGNORE INTO preferences(user_id) VALUES(?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void updateTheme(int userId, String theme) throws Exception {
        String sql = "UPDATE preferences SET theme = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, theme);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateTimerDurations(int userId, int focusMinutes, int shortBreakMinutes, int longBreakMinutes) throws Exception {
        String sql = """
            UPDATE preferences 
            SET focus_minutes = ?, short_break_minutes = ?, long_break_minutes = ? 
            WHERE user_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, focusMinutes);
            ps.setInt(2, shortBreakMinutes);
            ps.setInt(3, longBreakMinutes);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    public String getTaskFilter(int userId) throws Exception {
        String sql = "SELECT task_filter FROM preferences WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("task_filter");
            }
        }
        return null;
    }

    public void saveTaskFilter(int userId, String filter) throws Exception {
        ensureDefaultRow(userId);
        String sql = "UPDATE preferences SET task_filter = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filter);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updatePomodoroSettings(int userId, int focusMinutes, int shortBreakMinutes,
                                       int longBreakMinutes, int sessionsBeforeLongBreak,
                                       boolean enableSoundNotifications) throws Exception {
        ensureDefaultRow(userId);
        String sql = """
            UPDATE preferences
            SET focus_minutes = ?, short_break_minutes = ?, long_break_minutes = ?,
                sessions_before_long_break = ?, enable_sound_notifications = ?
            WHERE user_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, focusMinutes);
            ps.setInt(2, shortBreakMinutes);
            ps.setInt(3, longBreakMinutes);
            ps.setInt(4, sessionsBeforeLongBreak);
            ps.setInt(5, enableSoundNotifications ? 1 : 0);
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }
}
