package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML private Label welcomeLabel;

    @FXML
    private void initialize() {
        var u = Session.getCurrentUser();
        welcomeLabel.setText(u == null ? "Welcome" : "Welcome, " + u.getUsername());
    }

    @FXML
    private void onLogout() {
        Session.clear();
        try {
            App.setRoot("login");
        } catch (Exception ignored) {}
    }
}