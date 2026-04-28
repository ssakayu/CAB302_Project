package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class MainController {

    @FXML private Button widgetToggleButton;
    @FXML private ToggleGroup navGroup;

    @FXML
    private void initialize() {
        // Sync Launch Widget button label with actual widget state
        Session.get().widgetOpenProperty().addListener(
                (obs, wasOpen, isOpen) ->
                        widgetToggleButton.setText(isOpen ? "Close Widget" : "Launch Widget"));

        // Prevent clicking the active nav item from deselecting everything
        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        // Resize the stage to main-screen dimensions after the scene is ready
        Platform.runLater(() -> {
            Stage stage = (Stage) widgetToggleButton.getScene().getWindow();
            stage.setTitle("FocusDesk");
            stage.setWidth(900);
            stage.setHeight(620);
            stage.centerOnScreen();
        });
    }

    @FXML
    private void onNavSelect() {
        // content panel swapping wired here in later stories
    }

    @FXML
    private void toggleWidget() {
        Session session = Session.get();
        if (session.isWidgetOpen()) {
            session.closeWidget();
        } else {
            session.openWidget();
        }
    }

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
