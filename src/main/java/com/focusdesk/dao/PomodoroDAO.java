package com.focusdesk.dao;

import com.focusdesk.model.PomodoroSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PomodoroDAO {

    public void insert(int userId, int focusMinutes) throws Exception {
        String sql = "INSERT INTO pomodoro_sessions (user_id, focus_minutes) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, focusMinutes);
            stmt.executeUpdate();
        }
    }

    public List<PomodoroSession> getByUser(int userId) throws Exception {
        String sql = "SELECT id, user_id, focus_minutes FROM pomodoro_sessions WHERE user_id = ? ORDER BY completed_at DESC";
        List<PomodoroSession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(new PomodoroSession(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("focus_minutes")));
            }
        }
        return sessions;
    }

    public void deleteById(int sessionId) throws Exception {
        String sql = "DELETE FROM pomodoro_sessions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.executeUpdate();
        }
    }
}