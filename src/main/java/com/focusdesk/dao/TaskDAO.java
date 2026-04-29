package com.focusdesk.dao;

import com.focusdesk.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    public Task create(int userId, String title, String priority) throws Exception {
        String sql = "INSERT INTO tasks(user_id, title, priority) VALUES(?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, priority);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return new Task(rs.getInt(1), userId, title, false, priority);
            }
        }
        throw new SQLException("Failed to create task");
    }

    public List<Task> listByUser(int userId) throws Exception {
        String sql = "SELECT id, user_id, title, is_done, priority FROM tasks WHERE user_id = ? ORDER BY id DESC";
        List<Task> out = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRow(rs));
            }
        }
        return out;
    }

    public void setDone(int taskId, boolean done) throws Exception {
        String sql = "UPDATE tasks SET is_done = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, done ? 1 : 0);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void update(int taskId, String newTitle) throws Exception {
        String sql = "UPDATE tasks SET title = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void delete(int taskId) throws Exception {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }

    public void setPriority(int taskId, String priority) throws Exception {
        String sql = "UPDATE tasks SET priority = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, priority);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public List<Task> listByPriority(int userId, String priority) throws Exception {
        String sql = "SELECT id, user_id, title, is_done, priority FROM tasks " +
                     "WHERE user_id = ? AND priority = ? ORDER BY id DESC";
        List<Task> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, priority);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRow(rs));
            }
        }
        return out;
    }

    public List<Task> listByDone(int userId, boolean done) throws Exception {
        String sql = "SELECT id, user_id, title, is_done, priority FROM tasks " +
                     "WHERE user_id = ? AND is_done = ? ORDER BY id DESC";
        List<Task> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, done ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRow(rs));
            }
        }
        return out;
    }

    private Task fromRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("title"),
                rs.getInt("is_done") == 1,
                rs.getString("priority")
        );
    }
}