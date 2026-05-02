package com.focusdesk.controller;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.model.Preference;
import com.focusdesk.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class DashboardController {

    @FXML
    private VBox dashboardContainer;

    private Label timerLabel;
    private Label statusLabel;
    private Label sessionLabel;
    private Button startButton;
    private Button resetButton;
    private Button skipButton;
    private Button settingsButton;

    private int focusDuration;
    private int shortBreakDuration;
    private int longBreakDuration;
    private int sessionsBeforeLongBreak;
    private boolean enableSoundNotifications;

    private int remainingSeconds;
    private Timeline timeline;
    private boolean isRunning = false;
    private boolean isFocusSession = true;
    private int completedSessions = 0;

    @FXML
    private void initialize() {
        try {
            User currentUser = Session.get().getCurrentUser();
            if (currentUser == null) {
                throw new Exception("No user logged in");
            }

            Preference pref = new PreferenceDAO().getByUserId(currentUser.getId());
            if (pref != null) {
                focusDuration = pref.getFocusMinutes();
                shortBreakDuration = pref.getShortBreakMinutes();
                longBreakDuration = pref.getLongBreakMinutes();
                sessionsBeforeLongBreak = pref.getSessionsBeforeLongBreak();
                enableSoundNotifications = pref.isEnableSoundNotifications();
            } else {
                // Defaults
                focusDuration = 25;
                shortBreakDuration = 5;
                longBreakDuration = 15;
                sessionsBeforeLongBreak = 4;
                enableSoundNotifications = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            focusDuration = 25;
            shortBreakDuration = 5;
            longBreakDuration = 15;
            sessionsBeforeLongBreak = 4;
            enableSoundNotifications = true;
        }

        remainingSeconds = focusDuration * 60;
        setupUI();
        updateDisplay();
    }

    private void setupUI() {
        dashboardContainer.setSpacing(20);
        dashboardContainer.setPadding(new javafx.geometry.Insets(30));
        dashboardContainer.setStyle("-fx-padding: 30; -fx-spacing: 20; -fx-background-color: #ffffff;");

        // Title
        Label titleLabel = new Label("Dashboard");
        titleLabel.setStyle("-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        // Main content in 2x2 grid layout
        javafx.scene.layout.GridPane mainContent = createMainContent();

        dashboardContainer.getChildren().addAll(titleLabel, mainContent);
    }

    private javafx.scene.layout.GridPane createMainContent() {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setStyle("-fx-padding: 10 0 0 0;");

        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Ensure two rows split evenly
        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        row1.setPercentHeight(50);
        row2.setPercentHeight(50);
        grid.getRowConstraints().addAll(row1, row2);

        // Top-left: Pomodoro Timer
        VBox timerSection = createTimerSection();
        timerSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 25; -fx-background-color: #ffffff;");
        GridPane.setHgrow(timerSection, Priority.ALWAYS);
        GridPane.setVgrow(timerSection, Priority.ALWAYS);
        timerSection.setMaxHeight(Double.MAX_VALUE);
        grid.add(timerSection, 0, 0);

        // Top-right: Calendar
        VBox calendarSection = createCalendarSection();
        calendarSection.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #ffffff;");
        GridPane.setHgrow(calendarSection, Priority.ALWAYS);
        GridPane.setVgrow(calendarSection, Priority.ALWAYS);
        calendarSection.setMaxHeight(Double.MAX_VALUE);
        grid.add(calendarSection, 1, 0);

        // Bottom-left: Tasks
        VBox tasksBox = createTasksSection();
        tasksBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #ffffff;");
        GridPane.setHgrow(tasksBox, Priority.ALWAYS);
        GridPane.setVgrow(tasksBox, Priority.ALWAYS);
        tasksBox.setMaxHeight(Double.MAX_VALUE);
        grid.add(tasksBox, 0, 1);

        // Bottom-right: Notes
        VBox notesBox = createNotesSection();
        notesBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #ffffff;");
        GridPane.setHgrow(notesBox, Priority.ALWAYS);
        GridPane.setVgrow(notesBox, Priority.ALWAYS);
        notesBox.setMaxHeight(Double.MAX_VALUE);
        grid.add(notesBox, 1, 1);

        return grid;
    }

    private VBox createCalendarSection() {
        VBox calBox = new VBox(10);
        Label calTitle = new Label("Calendar");
        calTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label calDesc = new Label("View your upcoming events and study schedule");
        calDesc.setStyle("-fx-text-fill: #999; -fx-font-size: 12; -fx-wrap-text: true;");

        // Placeholder calendar area (replace with actual calendar control if available)
        Region calendarPlaceholder = new Region();
        calendarPlaceholder.setPrefHeight(220);
        calendarPlaceholder.setStyle("-fx-background-color: transparent;");

        calBox.getChildren().addAll(calTitle, calDesc, calendarPlaceholder);
        VBox.setVgrow(calendarPlaceholder, Priority.ALWAYS);
        return calBox;
    }

    private VBox createTasksSection() {
        VBox tasksBox = new VBox(10);
        Label tasksTitle = new Label("Tasks");
        tasksTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label tasksDesc = new Label("Connect this with your task list to pick a task before each session.");
        tasksDesc.setStyle("-fx-text-fill: #999; -fx-font-size: 12; -fx-wrap-text: true;");
        tasksBox.getChildren().addAll(tasksTitle, tasksDesc);
        VBox.setVgrow(tasksBox, Priority.ALWAYS);
        return tasksBox;
    }

    private VBox createNotesSection() {
        VBox notesBox = new VBox(10);
        Label notesTitle = new Label("Notes");
        notesTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label notesDesc = new Label("Keep quick notes while studying and review after each cycle.");
        notesDesc.setStyle("-fx-text-fill: #999; -fx-font-size: 12; -fx-wrap-text: true;");
        notesBox.getChildren().addAll(notesTitle, notesDesc);
        VBox.setVgrow(notesBox, Priority.ALWAYS);
        return notesBox;
    }

    private VBox createTimerSection() {
        VBox timerBox = new VBox(15);
        timerBox.setSpacing(15);

        // Timer title - at top left
        Label timerTitle = new Label("Pomodoro Timer");
        timerTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        timerTitle.setMaxWidth(Double.MAX_VALUE);

        // Centered container for display and controls
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setSpacing(15);

        // Timer display
        VBox displayBox = new VBox(15);
        displayBox.setAlignment(Pos.CENTER);
        displayBox.setStyle("-fx-padding: 0;");

        statusLabel = new Label("Focus");
        statusLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");

        // Create circular timer display
        VBox circleContainer = createCircleDisplay();

        sessionLabel = new Label("Session 1 of 4");
        sessionLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");

        displayBox.getChildren().addAll(statusLabel, circleContainer, sessionLabel);

        // Controls
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-spacing: 10;");

        startButton = new Button("Start");
        startButton.setPrefWidth(90);
        startButton.setStyle("-fx-font-size: 11; -fx-padding: 8 15; -fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");
        startButton.setOnAction(e -> toggleTimer());

        resetButton = new Button("Reset");
        resetButton.setPrefWidth(90);
        resetButton.setStyle("-fx-font-size: 11; -fx-padding: 8 15; -fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");
        resetButton.setOnAction(e -> resetTimer());

        skipButton = new Button("Skip");
        skipButton.setPrefWidth(90);
        skipButton.setStyle("-fx-font-size: 11; -fx-padding: 8 15; -fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");
        skipButton.setOnAction(e -> skipSession());

        settingsButton = new Button("⚙");
        settingsButton.setPrefWidth(50);
        settingsButton.setStyle("-fx-font-size: 14; -fx-padding: 8 12; -fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");
        settingsButton.setOnAction(e -> showSettingsDialog());

        controls.getChildren().addAll(startButton, resetButton, skipButton, settingsButton);

        centerContent.getChildren().addAll(displayBox, controls);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        timerBox.getChildren().addAll(timerTitle, centerContent);
        return timerBox;
    }

    private VBox createCircleDisplay() {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setPrefHeight(350);
        container.setSpacing(15);

        // Create a StackPane for centering content inside the circle
        StackPane circlePane = new StackPane();
        circlePane.setPrefSize(280, 280);
        circlePane.setMaxSize(280, 280);
        circlePane.setStyle(
            "-fx-border-color: #4CAF50; " +
            "-fx-border-width: 5; " +
            "-fx-background-color: #ffffff; " +
            "-fx-background-radius: 140; " +
            "-fx-border-radius: 140; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        );

        timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size: 80; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        circlePane.getChildren().add(timerLabel);

        container.getChildren().add(circlePane);
        return container;
    }

    private void updateDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));

        if (isFocusSession) {
            statusLabel.setText("Focus");
            statusLabel.setStyle("-fx-font-size: 18; -fx-text-fill: #4CAF50;");
        } else {
            statusLabel.setText("Break");
            statusLabel.setStyle("-fx-font-size: 18; -fx-text-fill: #2196F3;");
        }

        sessionLabel.setText(String.format("Session %d of %d", completedSessions + 1, sessionsBeforeLongBreak));
    }

    private void toggleTimer() {
        if (isRunning) {
            pauseTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        if (isRunning) return;

        isRunning = true;
        startButton.setText("Pause");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateDisplay();

            if (remainingSeconds <= 0) {
                sessionComplete();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pauseTimer() {
        if (timeline != null) {
            timeline.stop();
        }
        isRunning = false;
        startButton.setText("Start");
    }

    private void resetTimer() {
        pauseTimer();
        remainingSeconds = isFocusSession ? focusDuration * 60 : 
                          (completedSessions % sessionsBeforeLongBreak == 0 ? longBreakDuration : shortBreakDuration) * 60;
        updateDisplay();
    }

    private void skipSession() {
        pauseTimer();
        sessionComplete();
    }

    private void sessionComplete() {
        pauseTimer();

        if (isFocusSession) {
            completedSessions++;

            // Determine break length
            if (completedSessions % sessionsBeforeLongBreak == 0) {
                remainingSeconds = longBreakDuration * 60;
            } else {
                remainingSeconds = shortBreakDuration * 60;
            }

            isFocusSession = false;
            playNotification();
        } else {
            isFocusSession = true;
            remainingSeconds = focusDuration * 60;

            // If we've completed all sessions, reset
            if (completedSessions >= sessionsBeforeLongBreak) {
                completedSessions = 0;
            }

            playNotification();
        }

        updateDisplay();
    }

    private void playNotification() {
        if (enableSoundNotifications) {
            // Play a simple beep sound (you can implement actual audio later)
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private void showSettingsDialog() {
        pauseTimer();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Pomodoro Settings");
        dialog.setHeaderText(null);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 15;");

        // Focus Duration
        HBox focusBox = new HBox(10);
        focusBox.setAlignment(Pos.CENTER_LEFT);
        Label focusLabel = new Label("Focus Duration (min):");
        focusLabel.setPrefWidth(150);
        SpinnerValueFactory<Integer> focusFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, focusDuration);
        Spinner<Integer> focusSpinner = new Spinner<>(focusFactory);
        focusSpinner.setPrefWidth(80);
        focusBox.getChildren().addAll(focusLabel, focusSpinner);

        // Short Break
        HBox shortBreakBox = new HBox(10);
        shortBreakBox.setAlignment(Pos.CENTER_LEFT);
        Label shortBreakLabel = new Label("Short Break (min):");
        shortBreakLabel.setPrefWidth(150);
        SpinnerValueFactory<Integer> shortBreakFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, shortBreakDuration);
        Spinner<Integer> shortBreakSpinner = new Spinner<>(shortBreakFactory);
        shortBreakSpinner.setPrefWidth(80);
        shortBreakBox.getChildren().addAll(shortBreakLabel, shortBreakSpinner);

        // Long Break
        HBox longBreakBox = new HBox(10);
        longBreakBox.setAlignment(Pos.CENTER_LEFT);
        Label longBreakLabel = new Label("Long Break (min):");
        longBreakLabel.setPrefWidth(150);
        SpinnerValueFactory<Integer> longBreakFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, longBreakDuration);
        Spinner<Integer> longBreakSpinner = new Spinner<>(longBreakFactory);
        longBreakSpinner.setPrefWidth(80);
        longBreakBox.getChildren().addAll(longBreakLabel, longBreakSpinner);

        // Sessions Before Long Break
        HBox sessionsBox = new HBox(10);
        sessionsBox.setAlignment(Pos.CENTER_LEFT);
        Label sessionsLabel = new Label("Sessions Before Long Break:");
        sessionsLabel.setPrefWidth(150);
        SpinnerValueFactory<Integer> sessionsFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, sessionsBeforeLongBreak);
        Spinner<Integer> sessionsSpinner = new Spinner<>(sessionsFactory);
        sessionsSpinner.setPrefWidth(80);
        sessionsBox.getChildren().addAll(sessionsLabel, sessionsSpinner);

        // Sound Notifications
        HBox soundBox = new HBox(10);
        soundBox.setAlignment(Pos.CENTER_LEFT);
        Label soundLabel = new Label("Enable sound notifications:");
        soundLabel.setPrefWidth(150);
        CheckBox soundCheckBox = new CheckBox();
        soundCheckBox.setSelected(enableSoundNotifications);
        soundBox.getChildren().addAll(soundLabel, soundCheckBox);

        content.getChildren().addAll(focusBox, shortBreakBox, longBreakBox, sessionsBox, soundBox);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                focusDuration = focusSpinner.getValue();
                shortBreakDuration = shortBreakSpinner.getValue();
                longBreakDuration = longBreakSpinner.getValue();
                sessionsBeforeLongBreak = sessionsSpinner.getValue();
                enableSoundNotifications = soundCheckBox.isSelected();

                // Save to database
                try {
                    User currentUser = Session.get().getCurrentUser();
                    if (currentUser != null) {
                        new PreferenceDAO().updatePomodoroSettings(
                            currentUser.getId(),
                            focusDuration,
                            shortBreakDuration,
                            longBreakDuration,
                            sessionsBeforeLongBreak,
                            enableSoundNotifications
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Reset timer with new settings
                remainingSeconds = focusDuration * 60;
                updateDisplay();
                return true;
            }
            return false;
        });

        dialog.showAndWait();
    }
}
