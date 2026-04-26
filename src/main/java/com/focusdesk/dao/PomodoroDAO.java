package com.focusdesk.dao;

import com.focusdesk.model.PomodoroSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PomodoroDAO {

    public PomodoroSession logSession(int userId, int focusMinutes) throws Exception {
        String sql = "INSERT INTO pomodoro_sessions(user_id, focus_minutes) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, focusMinutes);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new PomodoroSession(rs.getInt(1), userId, focusMinutes);
                }
            }
        }
        throw new RuntimeException("Failed to log pomodoro session");
    }

    public List<PomodoroSession> listByUser(int userId) throws Exception {
        String sql = "SELECT id, user_id, focus_minutes FROM pomodoro_sessions WHERE user_id = ? ORDER BY id DESC";
        List<PomodoroSession> out = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PomodoroSession(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("focus_minutes")
                    ));
                }
            }
        }
        return out;
    }
}