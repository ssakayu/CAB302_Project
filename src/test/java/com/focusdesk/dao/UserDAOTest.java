package com.focusdesk.dao;

import com.focusdesk.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private static final String TEST_EMAIL    = "usertest@usertest.test";
    private static final String TEST_USERNAME = "usertest";

    private final UserDAO dao = new UserDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.init();
        purgeTestUser();
    }

    @AfterEach
    void tearDown() throws Exception {
        purgeTestUser();
    }

    private void purgeTestUser() throws Exception {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM users WHERE email = ? OR username = ?")) {
            ps.setString(1, TEST_EMAIL);
            ps.setString(2, TEST_USERNAME);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // create
    // =========================================================================

    @Test
    void createReturnsUserWithPositiveId() throws Exception {
        User user = dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        assertTrue(user.getId() > 0);
    }

    @Test
    void createReturnsUserWithCorrectFields() throws Exception {
        User user = dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        assertEquals(TEST_USERNAME, user.getUsername());
        assertEquals(TEST_EMAIL,    user.getEmail());
        assertEquals("hash",        user.getPasswordHash());
    }

    @Test
    void createPersistsUserToDatabase() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        assertNotNull(dao.findByEmail(TEST_EMAIL));
    }

    @Test
    void createDuplicateUsernamethrowsException() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        assertThrows(Exception.class,
                () -> dao.create(TEST_USERNAME, "other@other.test", "hash"));
    }

    // =========================================================================
    // findByUsername
    // =========================================================================

    @Test
    void findByUsernameReturnsCorrectUser() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        User found = dao.findByUsername(TEST_USERNAME);
        assertNotNull(found);
        assertEquals(TEST_USERNAME, found.getUsername());
    }

    @Test
    void findByUsernameReturnsNullForUnknownUsername() throws Exception {
        assertNull(dao.findByUsername("nobody"));
    }

    @Test
    void findByUsernameReturnsAllFields() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "myhash");
        User found = dao.findByUsername(TEST_USERNAME);
        assertNotNull(found);
        assertEquals(TEST_EMAIL, found.getEmail());
        assertEquals("myhash",   found.getPasswordHash());
    }

    // =========================================================================
    // findByEmail
    // =========================================================================

    @Test
    void findByEmailReturnsCorrectUser() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        User found = dao.findByEmail(TEST_EMAIL);
        assertNotNull(found);
        assertEquals(TEST_EMAIL, found.getEmail());
    }

    @Test
    void findByEmailReturnsNullForUnknownEmail() throws Exception {
        assertNull(dao.findByEmail("ghost@ghost.test"));
    }

    @Test
    void findByEmailReturnsAllFields() throws Exception {
        dao.create(TEST_USERNAME, TEST_EMAIL, "myhash");
        User found = dao.findByEmail(TEST_EMAIL);
        assertNotNull(found);
        assertEquals(TEST_USERNAME, found.getUsername());
        assertEquals("myhash",      found.getPasswordHash());
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Test
    void findByIdReturnsCorrectUser() throws Exception {
        User created = dao.create(TEST_USERNAME, TEST_EMAIL, "hash");
        User found   = dao.findById(created.getId());
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void findByIdReturnsNullForUnknownId() throws Exception {
        assertNull(dao.findById(Integer.MAX_VALUE));
    }

    @Test
    void findByIdReturnsAllFields() throws Exception {
        User created = dao.create(TEST_USERNAME, TEST_EMAIL, "myhash");
        User found   = dao.findById(created.getId());
        assertNotNull(found);
        assertEquals(TEST_USERNAME, found.getUsername());
        assertEquals(TEST_EMAIL,    found.getEmail());
        assertEquals("myhash",      found.getPasswordHash());
    }
}
