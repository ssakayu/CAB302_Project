package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import com.focusdesk.service.AuthService;
import com.focusdesk.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("Please enter your email and password");
            return;
        }

        messageLabel.setText("Logging in...");

        TaskRunner.run(
                () -> auth.login(email, pass),
                user -> {
                    Session.get().login(user);
                    messageLabel.setText("");
                    try {
                        App.setRoot("main");
                    } catch (Exception e) {
                        messageLabel.setText("Failed to open main screen");
                    }
                },
                err -> messageLabel.setText(err.getMessage()));
    }

    @FXML
    private void onGoSignup() {
        try {
            App.setRoot("signup");
        } catch (Exception e) {
            messageLabel.setText("Failed to open signup");
        }
    }
}