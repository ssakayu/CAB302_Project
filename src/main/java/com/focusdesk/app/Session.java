package com.focusdesk.app;

import com.focusdesk.controller.widget.WidgetWindowManager;
import com.focusdesk.model.Note;
import com.focusdesk.model.Task;
import com.focusdesk.model.User;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

public class Session {

    private static Session instance;

    // Auth state
    private User currentUser;
    private ObservableList<Task> tasks;
    private ObservableList<Note> notes;

    // Window refs
    private Stage mainStage;
    private final WidgetWindowManager widgetWindowManager = new WidgetWindowManager();

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    /** Convenience alias. */
    public static Session get() { return getInstance(); }

    public static User getCurrentUser() { return get().currentUser; }
    public static void setCurrentUser(User user) { get().login(user); }
    public static void clear() { get().logout(); }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    public void login(User user) {
        this.currentUser = user;
        this.tasks = FXCollections.observableArrayList();
        this.notes = FXCollections.observableArrayList();
    }

    public void logout() {
        widgetWindowManager.close();
        this.currentUser = null;
        this.tasks        = null;
        this.notes        = null;
    }

    // -------------------------------------------------------------------------
    // Main window
    // -------------------------------------------------------------------------

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
        this.widgetWindowManager.setOwnerStage(stage);
    }
    public Stage getMainStage()           { return mainStage; }

    // -------------------------------------------------------------------------
    // Widget window
    // -------------------------------------------------------------------------

    public void openWidget() {
        widgetWindowManager.open();
    }

    public void closeWidget() {
        widgetWindowManager.close();
    }

    public boolean isWidgetOpen() {
        return widgetWindowManager.isOpen();
    }

    public BooleanProperty widgetOpenProperty() { return widgetWindowManager.openProperty(); }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public ObservableList<Task> getTasks()  { return tasks; }
    public ObservableList<Note> getNotes()  { return notes; }
}
