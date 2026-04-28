package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {

    @FXML
    private Button widgetToggleButton;

    @FXML
    private void initialize() {
        // Keep button label in sync with actual widget state,
        // including when the user closes the widget via its own close button
        Session.get().widgetOpenProperty().addListener(
                (obs, wasOpen, isOpen) -> widgetToggleButton.setText(isOpen ? "Close Widget" : "Open Widget"));
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
