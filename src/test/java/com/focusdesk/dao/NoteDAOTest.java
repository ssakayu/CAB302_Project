package com.focusdesk.dao;

import com.focusdesk.model.Note;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteDAOTest {

    // Fixed emails make cleanup deterministic even after a crashed run
    private static final String USER1_EMAIL = "notetest1@notetest.test";
    private static final String USER2_EMAIL = "notetest2@notetest.test";

    private final NoteDAO dao = new NoteDAO();
    private int userId;   // primary test user, created fresh each test

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.init();
        purgeTestUsers();               // handle any crash leftovers
        userId = createUser("notetest1", USER1_EMAIL);
    }

    @AfterEach
    void tearDown() throws Exception {
        purgeTestUsers();               // CASCADE removes all their notes too
    }

    /** Removes both test users (and their notes via CASCADE). */
    private void purgeTestUsers() throws Exception {
        String sql = "DELETE FROM users WHERE email IN (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, USER1_EMAIL);
            ps.setString(2, USER2_EMAIL);
            ps.executeUpdate();
        }
    }

    /** Inserts a minimal user row and returns the generated id. */
    private int createUser(String username, String email) throws Exception {
        String sql = "INSERT INTO users(username, email, password_hash) VALUES(?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, "testhash");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Could not create test user: " + username);
    }

    // =========================================================================
    // insert
    // =========================================================================

    @Test
    void insertReturnsNoteWithPositiveId() throws Exception {
        Note note = dao.insert(userId, "Hello world");
        assertTrue(note.getId() > 0);
    }

    @Test
    void insertReturnsNoteWithCorrectUserIdAndContent() throws Exception {
        Note note = dao.insert(userId, "My content");
        assertEquals(userId, note.getUserId());
        assertEquals("My content", note.getContent());
    }

    @Test
    void insertedNoteAppearsInGetAll() throws Exception {
        dao.insert(userId, "Persisted note");
        List<Note> notes = dao.getAll(userId);
        assertTrue(notes.stream().anyMatch(n -> "Persisted note".equals(n.getContent())));
    }

    // =========================================================================
    // getAll
    // =========================================================================

    @Test
    void getAllReturnsEmptyListForNewUser() throws Exception {
        assertTrue(dao.getAll(userId).isEmpty());
    }

    @Test
    void getAllReturnsAllInsertedNotes() throws Exception {
        dao.insert(userId, "A");
        dao.insert(userId, "B");
        dao.insert(userId, "C");
        assertEquals(3, dao.getAll(userId).size());
    }

    @Test
    void getAllOrderedNewestFirst() throws Exception {
        dao.insert(userId, "First inserted");
        dao.insert(userId, "Second inserted");

        List<Note> notes = dao.getAll(userId);

        assertEquals("Second inserted", notes.get(0).getContent());
        assertEquals("First inserted",  notes.get(1).getContent());
    }

    @Test
    void getAllDoesNotReturnOtherUsersNotes() throws Exception {
        int user2 = createUser("notetest2", USER2_EMAIL);
        dao.insert(user2, "Private note");

        List<Note> user1Notes = dao.getAll(userId);
        assertTrue(user1Notes.stream().noneMatch(n -> "Private note".equals(n.getContent())));
    }

    // =========================================================================
    // update
    // =========================================================================

    @Test
    void updateChangesNoteContent() throws Exception {
        Note note = dao.insert(userId, "Original");
        dao.update(note.getId(), "Updated");

        List<Note> notes = dao.getAll(userId);
        assertEquals("Updated", notes.get(0).getContent());
    }

    @Test
    void updateSetsUpdatedAtTimestamp() throws Exception {
        Note note = dao.insert(userId, "To be updated");
        dao.update(note.getId(), "New content");

        String updatedAt;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT updated_at FROM notes WHERE id = ?")) {
            ps.setInt(1, note.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Note row not found after update");
                updatedAt = rs.getString("updated_at");
            }
        }
        assertNotNull(updatedAt, "updated_at should be set after update");
        assertFalse(updatedAt.isBlank());
    }

    @Test
    void updateDoesNotAffectSiblingNotes() throws Exception {
        Note keep   = dao.insert(userId, "Keep me");
        Note change = dao.insert(userId, "Change me");

        dao.update(change.getId(), "Changed");

        List<Note> notes = dao.getAll(userId);
        assertTrue(notes.stream().anyMatch(n -> "Keep me".equals(n.getContent())));
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Test
    void deleteRemovesNoteFromGetAll() throws Exception {
        Note note = dao.insert(userId, "Will be deleted");
        dao.delete(note.getId());

        List<Note> notes = dao.getAll(userId);
        assertTrue(notes.stream().noneMatch(n -> n.getId() == note.getId()));
    }

    @Test
    void deleteOnlyRemovesTargetNote() throws Exception {
        Note keep   = dao.insert(userId, "Keep");
        Note remove = dao.insert(userId, "Remove");

        dao.delete(remove.getId());

        List<Note> notes = dao.getAll(userId);
        assertEquals(1, notes.size());
        assertEquals(keep.getId(), notes.get(0).getId());
    }

    @Test
    void deleteNonExistentIdDoesNotThrow() {
        assertDoesNotThrow(() -> dao.delete(Integer.MAX_VALUE));
    }

    // =========================================================================
    // search
    // =========================================================================

    @Test
    void searchFindsNoteByKeyword() throws Exception {
        dao.insert(userId, "Buy groceries tomorrow");
        dao.insert(userId, "Call the dentist");

        List<Note> results = dao.search(userId, "groceries");

        assertEquals(1, results.size());
        assertTrue(results.get(0).getContent().contains("groceries"));
    }

    @Test
    void searchMatchesSubstring() throws Exception {
        dao.insert(userId, "Important deadline Friday");

        List<Note> results = dao.search(userId, "dead");

        assertEquals(1, results.size());
    }

    @Test
    void searchIsCaseInsensitiveForAscii() throws Exception {
        dao.insert(userId, "Meeting with Alice");

        List<Note> results = dao.search(userId, "alice");

        assertFalse(results.isEmpty(), "SQLite LIKE should match ASCII case-insensitively");
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() throws Exception {
        dao.insert(userId, "Hello world");

        List<Note> results = dao.search(userId, "zzznomatch");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchReturnsMultipleMatchingNotes() throws Exception {
        dao.insert(userId, "Project alpha plan");
        dao.insert(userId, "Project beta plan");
        dao.insert(userId, "Unrelated note");

        List<Note> results = dao.search(userId, "Project");

        assertEquals(2, results.size());
    }

    @Test
    void searchDoesNotReturnOtherUsersNotes() throws Exception {
        int user2 = createUser("notetest2", USER2_EMAIL);
        dao.insert(user2, "User2 secret note");

        List<Note> results = dao.search(userId, "secret");

        assertTrue(results.isEmpty());
    }

    // =========================================================================
    // Backward-compat aliases
    // =========================================================================

    @Test
    void createAliasBehavesLikeInsert() throws Exception {
        Note note = dao.create(userId, "Via create alias");

        assertTrue(note.getId() > 0);
        assertEquals(userId, note.getUserId());
        assertEquals("Via create alias", note.getContent());
    }

    @Test
    void listByUserAliasBehavesLikeGetAll() throws Exception {
        dao.insert(userId, "One");
        dao.insert(userId, "Two");

        assertEquals(dao.getAll(userId).size(), dao.listByUser(userId).size());
    }
}
