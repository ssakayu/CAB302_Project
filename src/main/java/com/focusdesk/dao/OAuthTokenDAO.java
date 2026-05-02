package com.focusdesk.dao;

import com.focusdesk.model.OAuthToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OAuthTokenDAO {

    public OAuthToken find(int userId, String provider) throws Exception {
        String sql = """
                SELECT user_id, provider, access_token, refresh_token, token_type, scope, expires_at
                FROM oauth_tokens
                WHERE user_id = ? AND provider = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, provider);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new OAuthToken(
                        rs.getInt("user_id"),
                        rs.getString("provider"),
                        rs.getString("access_token"),
                        rs.getString("refresh_token"),
                        rs.getString("token_type"),
                        rs.getString("scope"),
                        rs.getString("expires_at")
                );
            }
        }
    }

    public void upsert(OAuthToken token) throws Exception {
        String sql = """
                INSERT INTO oauth_tokens (user_id, provider, access_token, refresh_token, token_type, scope, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id, provider) DO UPDATE SET
                    access_token = excluded.access_token,
                    refresh_token = COALESCE(excluded.refresh_token, oauth_tokens.refresh_token),
                    token_type = excluded.token_type,
                    scope = excluded.scope,
                    expires_at = excluded.expires_at
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, token.userId());
            ps.setString(2, token.provider());
            ps.setString(3, token.accessToken());
            ps.setString(4, token.refreshToken());
            ps.setString(5, token.tokenType());
            ps.setString(6, token.scope());
            ps.setString(7, token.expiresAt());
            ps.executeUpdate();
        }
    }

    public void delete(int userId, String provider) throws Exception {
        String sql = "DELETE FROM oauth_tokens WHERE user_id = ? AND provider = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, provider);
            ps.executeUpdate();
        }
    }


}


