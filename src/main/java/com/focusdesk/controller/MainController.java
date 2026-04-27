package com.focusdesk.controller;

import com.focusdesk.app.App;
import com.focusdesk.app.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;


public class MainController {

    @FXML private Label welcomeLabel;
    @FXML private Label sectionTitleLabel;
    @FXML private Label sectionHintLabel;
    @FXML private Button widgetToggleButton;
    @FXML private StackPane mainContentHost;

    @FXML
    private void initialize() {
        var u = Session.getCurrentUser();
        if (welcomeLabel != null) {
            welcomeLabel.setText(u == null ? "Welcome" : "Welcome, " + u.getUsername());
        }

        if (Session.get().getMainStage() != null) {
            Session.get().getMainStage().setTitle("FocusDesk");
            Session.get().getMainStage().setMinWidth(980);
            Session.get().getMainStage().setMinHeight(640);
            Session.get().getMainStage().setWidth(1140);
            Session.get().getMainStage().setHeight(720);
        }

        if (widgetToggleButton != null) {
            widgetToggleButton.setText(Session.get().isWidgetOpen() ? "Close Widget" : "Open Widget");
            Session.get().widgetOpenProperty().addListener((obs, wasOpen, isOpen) ->
                    widgetToggleButton.setText(isOpen ? "Close Widget" : "Open Widget")
            );
        }

        showOverview();
    }

    @FXML
    private void showOverview() {
        if (sectionTitleLabel != null) {
            sectionTitleLabel.setText("Dashboard");
        }
        if (sectionHintLabel != null) {
            sectionHintLabel.setText("Today at a glance");
        }
        loadSlide("/fxml/notes_slide.fxml");
    }

    @FXML
    private void showTodo() {
        sectionTitleLabel.setText("Tasks");
        sectionHintLabel.setText("Track priorities and progress");
        loadSlide("/fxml/todo_slide.fxml");
    }

    @FXML
    private void showTimer() {
        sectionTitleLabel.setText("Pomodoro");
        sectionHintLabel.setText("Focus blocks with quick controls");
        loadSlide("/fxml/timer_slide.fxml");
    }

    @FXML
    private void showCalendar() {
        sectionTitleLabel.setText("Calendar");
        sectionHintLabel.setText("Upcoming events and schedule");
        loadSlide("/fxml/calendar_slide.fxml");
    }

    @FXML
    private void showMusic() {
        sectionTitleLabel.setText("Music");
        sectionHintLabel.setText("Background audio for deep work");
        loadSlide("/fxml/music_slide.fxml");
    }

    @FXML
    private void showNotes() {
        sectionTitleLabel.setText("Notes");
        sectionHintLabel.setText("Capture ideas and study snippets");
        loadSlide("/fxml/notes_slide.fxml");
    }

    private void loadSlide(String resourcePath) {
        try {
            if (mainContentHost == null) {
                throw new IllegalStateException("mainContentHost was not injected from main.fxml");
            }

            mainContentHost.getChildren().clear();
            mainContentHost.getChildren().add(FXMLLoader.load(getClass().getResource(resourcePath)));
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("Unable to load view: " + resourcePath);
            mainContentHost.getChildren().clear();
            mainContentHost.getChildren().add(error);
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
