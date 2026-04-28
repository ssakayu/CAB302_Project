package com.focusdesk.dao;

import com.focusdesk.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Note fromRow(ResultSet rs) throws SQLException {
        return new Note(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("content"),
                rs.getString("created_at")
        );
    }

    // -------------------------------------------------------------------------
    // Insert
    // -------------------------------------------------------------------------

    public Note insert(int userId, String content) throws Exception {
        String sql = "INSERT INTO notes(user_id, content) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, content);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return new Note(rs.getInt(1), userId, content, null);
            }
        }
        throw new SQLException("Failed to insert note");
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    public void update(int noteId, String content) throws Exception {
        String sql = "UPDATE notes SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content);
            ps.setInt(2, noteId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    public void delete(int noteId) throws Exception {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Get all for user (newest first)
    // -------------------------------------------------------------------------

    public List<Note> getAll(int userId) throws Exception {
        String sql = "SELECT id, user_id, content, created_at FROM notes " +
                     "WHERE user_id = ? ORDER BY id DESC";
        List<Note> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRow(rs));
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Search by content (case-insensitive LIKE)
    // -------------------------------------------------------------------------

    public List<Note> search(int userId, String query) throws Exception {
        String sql = "SELECT id, user_id, content, created_at FROM notes " +
                     "WHERE user_id = ? AND content LIKE ? ORDER BY id DESC";
        List<Note> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(fromRow(rs));
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Backward-compat aliases used by Session.java
    // -------------------------------------------------------------------------

    public Note create(int userId, String content) throws Exception {
        return insert(userId, content);
    }

    public List<Note> listByUser(int userId) throws Exception {
        return getAll(userId);
    }
}
