package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {

    @FXML private Label welcomeLabel;
    @FXML private Button widgetToggleButton;

    @FXML
    private void initialize() {
        var u = Session.getCurrentUser();
        if (welcomeLabel != null) {
            welcomeLabel.setText(u == null ? "Welcome" : "Welcome, " + u.getUsername());
        }

        if (widgetToggleButton != null) {
            widgetToggleButton.setText(Session.get().isWidgetOpen() ? "Close Widget" : "Open Widget");
            Session.get().widgetOpenProperty().addListener((obs, wasOpen, isOpen) ->
                    widgetToggleButton.setText(isOpen ? "Close Widget" : "Open Widget")
            );
        }
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
        Session.clear();
        try {
            App.setRoot("login");
        } catch (Exception ignored) {}
    }
}
