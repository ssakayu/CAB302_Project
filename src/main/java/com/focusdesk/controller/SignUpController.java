package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.service.AuthService;
import com.focusdesk.util.TaskRunner;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignUpController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label messageLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void onSignup() {
        String username = usernameField.getText();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmField.getText();

        if (!pass.equals(confirm)) {
            messageLabel.setText("Passwords do not match");
            return;
        }

        messageLabel.setText("Creating account...");

        TaskRunner.run(
                () -> auth.signUp(username, email, pass),
                user -> {
                    messageLabel.setText("✅ Account created. Please login.");
                    try {
                        App.setRoot("login");
                    } catch (Exception e) {
                        messageLabel.setText("Created, but failed to go login");
                    }
                },
                err -> messageLabel.setText(err.getMessage())
        );
    }

    @FXML
    private void onGoLogin() {
        try {
            App.setRoot("login");
        } catch (Exception e) {
            messageLabel.setText("Failed to open login");
        }
    }
}