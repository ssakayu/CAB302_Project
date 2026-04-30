package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private Button widgetToggleButton;
    @FXML
    private ToggleGroup navGroup;
    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        Session.get().widgetOpenProperty().addListener(
                (obs, wasOpen, isOpen) -> widgetToggleButton.setText(isOpen ? "Close Widget" : "Launch Widget"));

        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                oldVal.setSelected(true);
        });

        Platform.runLater(() -> {
            Stage stage = (Stage) widgetToggleButton.getScene().getWindow();
            stage.setTitle("FocusDesk");
            stage.setWidth(1280);
            stage.setHeight(760);
            stage.centerOnScreen();
        });
    }

    // -------------------------------------------------------------------------
    // Nav switching
    // -------------------------------------------------------------------------

    @FXML
    private void onNavSelect() {
        Toggle selected = navGroup.getSelectedToggle();
        if (!(selected instanceof ToggleButton tb))
            return;

        switch (tb.getText()) {
            case "Tasks" -> loadPage("tasks_page");
            case "Calendar" -> loadPage("calendar_slide");
            case "Notes" -> loadPage("notes_page");
            default -> contentArea.getChildren().clear();
        }
    }

    private void loadPage(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxmlName + ".fxml"));
            Parent page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Label err = new javafx.scene.control.Label(
                    "Failed to load page: " + e.getMessage());
            err.setStyle("-fx-text-fill: red; -fx-font-size: 13px; -fx-padding: 20;");
            contentArea.getChildren().setAll(err);
        }
    }

    // -------------------------------------------------------------------------
    // Widget
    // -------------------------------------------------------------------------

    @FXML
    private void toggleWidget() {
        Session session = Session.get();
        if (session.isWidgetOpen())
            session.closeWidget();
        else
            session.openWidget();
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @FXML
    private void onLogout() {
        Session.get().logout();
        try {
            App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
