package com.focusdesk.app;

import com.focusdesk.model.Note;
import com.focusdesk.model.Task;
import com.focusdesk.model.User;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Session {

    private static Session instance;

    // Auth state
    private User currentUser;
    private ObservableList<Task> tasks;
    private ObservableList<Note> notes;

    // Window refs
    private Stage mainStage;
    private Stage widgetStage;

    // Reactive open-state so controllers can bind without polling
    private final BooleanProperty widgetOpen = new SimpleBooleanProperty(false);

    // Last known widget geometry (in-memory only).
    // TODO: load initial values from PreferenceDAO on login; persist on logout/close via PreferenceDAO
    private double widgetX      = Double.NaN;
    private double widgetY      = Double.NaN;
    private double widgetWidth  = 380;
    private double widgetHeight = 220;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    /** Convenience alias. */
    public static Session get() { return getInstance(); }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    public void login(User user) {
        this.currentUser = user;
        this.tasks = FXCollections.observableArrayList();
        this.notes = FXCollections.observableArrayList();
    }

    public void logout() {
        closeWidget();
        this.currentUser = null;
        this.tasks        = null;
        this.notes        = null;
    }

    // -------------------------------------------------------------------------
    // Main window
    // -------------------------------------------------------------------------

    public void setMainStage(Stage stage) { this.mainStage = stage; }
    public Stage getMainStage()           { return mainStage; }

    // -------------------------------------------------------------------------
    // Widget window
    // -------------------------------------------------------------------------

    /**
     * Opens the floating widget window.
     * If already open, brings it to the front instead of opening a second instance.
     * Must be called on the JavaFX application thread.
     */
    public void openWidget() {
        if (isWidgetOpen()) {
            widgetStage.toFront();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/widget.fxml"));
            Scene scene = new Scene(loader.load());

            widgetStage = new Stage();
            widgetStage.initStyle(StageStyle.UNDECORATED);
            widgetStage.initOwner(mainStage);   // keeps it off the taskbar
            widgetStage.setAlwaysOnTop(true);
            widgetStage.setTitle("FocusDesk Widget");

            widgetStage.setMinWidth(300);
            widgetStage.setMinHeight(150);
            widgetStage.setMaxWidth(500);
            widgetStage.setMaxHeight(300);

            // Restore last known size
            widgetStage.setWidth(widgetWidth);
            widgetStage.setHeight(widgetHeight);

            // Restore last known position (NaN means first open — let the OS decide)
            if (!Double.isNaN(widgetX)) {
                widgetStage.setX(widgetX);
                widgetStage.setY(widgetY);
            }

            // Track live geometry so memory stays current as the user moves/resizes
            widgetStage.xProperty().addListener((obs, o, n)      -> widgetX      = n.doubleValue());
            widgetStage.yProperty().addListener((obs, o, n)      -> widgetY      = n.doubleValue());
            widgetStage.widthProperty().addListener((obs, o, n)  -> widgetWidth  = n.doubleValue());
            widgetStage.heightProperty().addListener((obs, o, n) -> widgetHeight = n.doubleValue());

            // Persist final geometry when the window is about to hide
            widgetStage.setOnHiding(e -> {
                widgetX      = widgetStage.getX();
                widgetY      = widgetStage.getY();
                widgetWidth  = widgetStage.getWidth();
                widgetHeight = widgetStage.getHeight();
                widgetOpen.set(false);
                // TODO: call PreferenceDAO.updateWidgetBounds(currentUser.getId(), widgetX, widgetY, widgetWidth, widgetHeight)
            });

            widgetStage.setOnHidden(e -> widgetStage = null);

            widgetStage.setScene(scene);
            widgetStage.show();
            widgetOpen.set(true);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load widget.fxml", e);
        }
    }

    /**
     * Closes the widget window if open.
     * Must be called on the JavaFX application thread.
     */
    public void closeWidget() {
        if (widgetStage != null) {
            widgetStage.hide();  // triggers setOnHiding then setOnHidden
        }
    }

    public boolean isWidgetOpen() {
        return widgetStage != null && widgetStage.isShowing();
    }

    public BooleanProperty widgetOpenProperty() { return widgetOpen; }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public User getCurrentUser()            { return currentUser; }
    public ObservableList<Task> getTasks()  { return tasks; }
    public ObservableList<Note> getNotes()  { return notes; }
}
