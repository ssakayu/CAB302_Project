package com.focusdesk.dao;

import com.focusdesk.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserDaoTest {

    private final UserDAO userDAO = new UserDAO();
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.init();
        String token = String.valueOf(System.nanoTime());
        testUser = userDAO.create(
                "userdao_" + token,
                "userdao_" + token + "@example.com",
                "hash-" + token
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testUser != null) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setInt(1, testUser.getId());
                ps.executeUpdate();
            }
        }
    }

    @Test
    void createFindsUserByEmail() throws Exception {
        User found = userDAO.findByEmail(testUser.getEmail());

        assertNotNull(found);
        assertEquals(testUser.getId(), found.getId());
        assertEquals(testUser.getUsername(), found.getUsername());
    }
}
