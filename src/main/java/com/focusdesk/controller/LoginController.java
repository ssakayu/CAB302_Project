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

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final AuthService auth = new AuthService();

    @FXML
    private void onLogin() {
        messageLabel.setText("Logging in...");

        String email = emailField.getText().trim();
        String pass = passwordField.getText();

        TaskRunner.run(
                () -> auth.login(email, pass),
                user -> {
                    Session.setCurrentUser(user);
                    messageLabel.setText("");
                    try {
                        App.setRoot("main");
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageLabel.setText("Failed to open main screen: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                },
                err -> messageLabel.setText(err.getMessage())
        );
    }

    @FXML
    private void onGoSignup() {
        try {
            App.setRoot("signup");
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Failed to open signup: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
