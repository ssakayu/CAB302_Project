package com.focusdesk.dao;

import com.focusdesk.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    public Note create(int userId, String content) throws Exception {
        String sql = "INSERT INTO notes(user_id, content) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, content);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return new Note(rs.getInt(1), userId, content);
            }
        }
        throw new SQLException("Failed to create note");
    }

    public List<Note> listByUser(int userId) throws Exception {
        String sql = "SELECT id, user_id, content FROM notes WHERE user_id = ? ORDER BY id DESC";
        List<Note> out = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Note(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("content")
                    ));
                }
            }
        }
        return out;
    }
}