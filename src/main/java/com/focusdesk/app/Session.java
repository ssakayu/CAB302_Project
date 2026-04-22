package com.focusdesk.app;

import com.focusdesk.model.Note;
import com.focusdesk.model.Task;
import com.focusdesk.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Session {

    private static Session instance;

    private User currentUser;
    private ObservableList<Task> tasks;
    private ObservableList<Note> notes;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        this.tasks = FXCollections.observableArrayList();
        this.notes = FXCollections.observableArrayList();
    }

    public void logout() {
        this.currentUser = null;
        this.tasks = null;
        this.notes = null;
    }

    public User getCurrentUser() { return currentUser; }
    public ObservableList<Task> getTasks() { return tasks; }
    public ObservableList<Note> getNotes() { return notes; }
}
