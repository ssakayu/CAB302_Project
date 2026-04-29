package com.focusdesk.controller.widget;

import com.focusdesk.service.PomodoroService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;

/**
 * Controller for the Pomodoro timer slide in the widget.
 * Displays countdown, phase info, and provides start/pause/reset/skip controls.
 */
public class TimerSlideController {

    @FXML private StackPane timerContainer;
    @FXML private Label phaseLabel;
    @FXML private Label timeLabel;
    @FXML private Button startPauseBtn;
    @FXML private Button resetBtn;
    @FXML private Button skipBtn;
    @FXML private Button settingsBtn;

    private PomodoroService pomodoroService;
    private Arc progressRing;

    @FXML
    public void initialize() {
        pomodoroService = new PomodoroService();

        // Bind labels to service properties
        phaseLabel.textProperty().bind(pomodoroService.phaseLabelProperty());
        startPauseBtn.textProperty().bind(pomodoroService.buttonLabelProperty());

        // Bind time display (MM:SS format)
        pomodoroService.secondsRemainingProperty().addListener((obs, oldVal, newVal) -> {
            int seconds = newVal.intValue();
            int minutes = seconds / 60;
            int secs = seconds % 60;
            timeLabel.setText(String.format("%02d:%02d", minutes, secs));
            updateProgressRing(newVal.intValue());
        });

        // Initialize time display
        int initial = pomodoroService.secondsRemainingProperty().get();
        int minutes = initial / 60;
        int secs = initial % 60;
        timeLabel.setText(String.format("%02d:%02d", minutes, secs));

        // Setup progress ring (visual circular indicator)
        setupProgressRing();

        // Setup phase complete callback for notifications
        pomodoroService.setOnPhaseComplete(() -> {
            // Could trigger sound or notification here
            System.out.println("Phase complete! " + phaseLabel.getText());
        });
    }

    /**
     * Create and add a circular progress ring to the timer display.
     */
    private void setupProgressRing() {
        progressRing = new Arc(60, 60, 50, 50, 0, 360);
        progressRing.setStrokeWidth(3);
        progressRing.setStroke(Color.web("#4a7c8c"));
        progressRing.setFill(null);
        progressRing.setType(javafx.scene.shape.ArcType.OPEN);

        // Add behind time label for visual effect
        timerContainer.getChildren().add(0, progressRing);
    }

    /**
     * Update the progress ring arc length based on time remaining.
     */
    private void updateProgressRing(int secondsRemaining) {
        int totalSeconds = pomodoroService.getFocusMinutes() * 60; // Use current phase duration
        double progress = (double) secondsRemaining / totalSeconds;
        progressRing.setLength(360 * progress);
    }

    @FXML
    private void onStartPause() {
        pomodoroService.startPause();
    }

    @FXML
    private void onReset() {
        pomodoroService.reset();
    }

    @FXML
    private void onSkip() {
        pomodoroService.skipPhase();
    }

    @FXML
    private void onSettings() {
        System.out.println("Settings clicked - open customization dialog");
    }
}
