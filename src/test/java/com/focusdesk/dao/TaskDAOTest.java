package com.focusdesk.dao;

import com.focusdesk.model.Task;
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

class TaskDAOTest {

    private static final String USER1_EMAIL = "tasktest1@tasktest.test";
    private static final String USER2_EMAIL = "tasktest2@tasktest.test";

    private final TaskDAO dao = new TaskDAO();
    private int userId;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.init();
        purgeTestUsers();
        userId = createUser("tasktest1", USER1_EMAIL);
    }

    @AfterEach
    void tearDown() throws Exception {
        purgeTestUsers();
    }

    private void purgeTestUsers() throws Exception {
        String sql = "DELETE FROM users WHERE email IN (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, USER1_EMAIL);
            ps.setString(2, USER2_EMAIL);
            ps.executeUpdate();
        }
    }

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
    // create
    // =========================================================================

    @Test
    void createReturnsTaskWithPositiveId() throws Exception {
        Task task = dao.create(userId, "Buy milk", "low");
        assertTrue(task.getId() > 0);
    }

    @Test
    void createReturnsTaskWithCorrectFields() throws Exception {
        Task task = dao.create(userId, "Write tests", "high");
        assertEquals(userId, task.getUserId());
        assertEquals("Write tests", task.getTitle());
        assertEquals("high", task.getPriority());
    }

    @Test
    void createDefaultsDoneToFalse() throws Exception {
        Task task = dao.create(userId, "Fresh task", "medium");
        assertFalse(task.isDone());
    }

    @Test
    void createdTaskAppearsInListByUser() throws Exception {
        dao.create(userId, "Persisted task", "medium");
        List<Task> tasks = dao.listByUser(userId);
        assertTrue(tasks.stream().anyMatch(t -> "Persisted task".equals(t.getTitle())));
    }

    // =========================================================================
    // listByUser
    // =========================================================================

    @Test
    void listByUserReturnsEmptyListForNewUser() throws Exception {
        assertTrue(dao.listByUser(userId).isEmpty());
    }

    @Test
    void listByUserReturnsAllCreatedTasks() throws Exception {
        dao.create(userId, "A", "low");
        dao.create(userId, "B", "medium");
        dao.create(userId, "C", "high");
        assertEquals(3, dao.listByUser(userId).size());
    }

    @Test
    void listByUserOrderedNewestFirst() throws Exception {
        dao.create(userId, "First", "low");
        dao.create(userId, "Second", "low");
        List<Task> tasks = dao.listByUser(userId);
        assertEquals("Second", tasks.get(0).getTitle());
        assertEquals("First",  tasks.get(1).getTitle());
    }

    @Test
    void listByUserDoesNotReturnOtherUsersTasks() throws Exception {
        int user2 = createUser("tasktest2", USER2_EMAIL);
        dao.create(user2, "Private task", "high");

        List<Task> user1Tasks = dao.listByUser(userId);
        assertTrue(user1Tasks.stream().noneMatch(t -> "Private task".equals(t.getTitle())));
    }

    // =========================================================================
    // setDone
    // =========================================================================

    @Test
    void setDoneMarksDone() throws Exception {
        Task task = dao.create(userId, "Finish report", "medium");
        dao.setDone(task.getId(), true);

        Task loaded = dao.listByUser(userId).get(0);
        assertTrue(loaded.isDone());
    }

    @Test
    void setDoneCanUndone() throws Exception {
        Task task = dao.create(userId, "Was done", "medium");
        dao.setDone(task.getId(), true);
        dao.setDone(task.getId(), false);

        Task loaded = dao.listByUser(userId).get(0);
        assertFalse(loaded.isDone());
    }

    @Test
    void setDoneDoesNotAffectSiblingTasks() throws Exception {
        Task keep   = dao.create(userId, "Keep untouched", "medium");
        Task change = dao.create(userId, "Mark done",      "medium");

        dao.setDone(change.getId(), true);

        List<Task> tasks = dao.listByUser(userId);
        Task reloadedKeep = tasks.stream()
                .filter(t -> t.getId() == keep.getId()).findFirst().orElseThrow();
        assertFalse(reloadedKeep.isDone());
    }

    // =========================================================================
    // update  (RED — method does not exist yet)
    // =========================================================================

    @Test
    void updateChangesTitle() throws Exception {
        Task task = dao.create(userId, "Old title", "medium");
        dao.update(task.getId(), "New title");

        Task loaded = dao.listByUser(userId).get(0);
        assertEquals("New title", loaded.getTitle());
    }

    @Test
    void updateDoesNotAffectSiblingTasks() throws Exception {
        Task keep   = dao.create(userId, "Sibling task", "medium");
        Task change = dao.create(userId, "To be renamed", "medium");

        dao.update(change.getId(), "Renamed");

        List<Task> tasks = dao.listByUser(userId);
        assertTrue(tasks.stream().anyMatch(t -> "Sibling task".equals(t.getTitle())));
    }

    // =========================================================================
    // delete  (RED — method does not exist yet)
    // =========================================================================

    @Test
    void deleteRemovesFromListByUser() throws Exception {
        Task task = dao.create(userId, "Will be deleted", "medium");
        dao.delete(task.getId());

        List<Task> tasks = dao.listByUser(userId);
        assertTrue(tasks.stream().noneMatch(t -> t.getId() == task.getId()));
    }

    @Test
    void deleteOnlyRemovesTargetTask() throws Exception {
        Task keep   = dao.create(userId, "Keep me",   "medium");
        Task remove = dao.create(userId, "Remove me", "medium");

        dao.delete(remove.getId());

        List<Task> tasks = dao.listByUser(userId);
        assertEquals(1, tasks.size());
        assertEquals(keep.getId(), tasks.get(0).getId());
    }

    @Test
    void deleteNonExistentIdDoesNotThrow() {
        assertDoesNotThrow(() -> dao.delete(Integer.MAX_VALUE));
    }

    // =========================================================================
    // setPriority  (RED — method does not exist yet)
    // =========================================================================

    @Test
    void setPriorityChangesToHigh() throws Exception {
        Task task = dao.create(userId, "Escalate me", "low");
        dao.setPriority(task.getId(), "high");

        Task loaded = dao.listByUser(userId).get(0);
        assertEquals("high", loaded.getPriority());
    }

    @Test
    void setPriorityChangesToLow() throws Exception {
        Task task = dao.create(userId, "Downgrade me", "high");
        dao.setPriority(task.getId(), "low");

        Task loaded = dao.listByUser(userId).get(0);
        assertEquals("low", loaded.getPriority());
    }

    @Test
    void setPriorityDoesNotAffectSiblingTasks() throws Exception {
        Task sibling = dao.create(userId, "Sibling high", "high");
        Task target  = dao.create(userId, "To change",    "high");

        dao.setPriority(target.getId(), "low");

        List<Task> tasks = dao.listByUser(userId);
        Task reloadedSibling = tasks.stream()
                .filter(t -> t.getId() == sibling.getId()).findFirst().orElseThrow();
        assertEquals("high", reloadedSibling.getPriority());
    }

    // =========================================================================
    // listByPriority  (RED — method does not exist yet)
    // =========================================================================

    @Test
    void listByPriorityFiltersCorrectly() throws Exception {
        dao.create(userId, "High priority task", "high");
        dao.create(userId, "Low priority task",  "low");

        List<Task> results = dao.listByPriority(userId, "high");

        assertEquals(1, results.size());
        assertEquals("high", results.get(0).getPriority());
    }

    @Test
    void listByPriorityReturnsMultipleMatches() throws Exception {
        dao.create(userId, "High A", "high");
        dao.create(userId, "High B", "high");
        dao.create(userId, "Low C",  "low");

        List<Task> results = dao.listByPriority(userId, "high");
        assertEquals(2, results.size());
    }

    @Test
    void listByPriorityReturnsEmptyWhenNoMatch() throws Exception {
        dao.create(userId, "Only medium", "medium");

        List<Task> results = dao.listByPriority(userId, "high");
        assertTrue(results.isEmpty());
    }

    @Test
    void listByPriorityDoesNotReturnOtherUsersTasks() throws Exception {
        int user2 = createUser("tasktest2", USER2_EMAIL);
        dao.create(user2, "User2 high task", "high");

        List<Task> results = dao.listByPriority(userId, "high");
        assertTrue(results.isEmpty());
    }

    // =========================================================================
    // listByDone  (RED — method does not exist yet)
    // =========================================================================

    @Test
    void listByDoneReturnsDoneTasks() throws Exception {
        Task done       = dao.create(userId, "Done task",   "medium");
        Task incomplete = dao.create(userId, "Open task",   "medium");
        dao.setDone(done.getId(), true);

        List<Task> results = dao.listByDone(userId, true);
        assertEquals(1, results.size());
        assertEquals(done.getId(), results.get(0).getId());
    }

    @Test
    void listByDoneReturnsIncompleteTasks() throws Exception {
        Task done       = dao.create(userId, "Done task",   "medium");
        Task incomplete = dao.create(userId, "Open task",   "medium");
        dao.setDone(done.getId(), true);

        List<Task> results = dao.listByDone(userId, false);
        assertEquals(1, results.size());
        assertEquals(incomplete.getId(), results.get(0).getId());
    }

    @Test
    void listByDoneDoesNotReturnOtherUsersTasks() throws Exception {
        int user2 = createUser("tasktest2", USER2_EMAIL);
        Task t = dao.create(user2, "User2 done", "medium");
        dao.setDone(t.getId(), true);

        List<Task> results = dao.listByDone(userId, true);
        assertTrue(results.isEmpty());
    }
}
