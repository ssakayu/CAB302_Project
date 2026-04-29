package com.focusdesk.controller.widget;

import com.focusdesk.service.PomodoroService;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the Pomodoro timer slide in the widget.
 * Displays countdown, phase info, and provides start/pause/reset/skip controls.
 */
public class TimerSlideController {

    private static final double RING_PADDING = 18;

    @FXML private StackPane timerContainer;
    @FXML private Label phaseLabel;
    @FXML private Label sessionLabel;
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

        // Setup progress ring first so initial render does not hit null.
        setupProgressRing();

        // Keep ring size synced with label size across different dashboard/widget layouts.
        timeLabel.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateRingGeometry());

        // Bind labels to service properties
        phaseLabel.textProperty().bind(pomodoroService.phaseLabelProperty());
        sessionLabel.textProperty().bind(pomodoroService.sessionLabelProperty());
        startPauseBtn.textProperty().bind(pomodoroService.buttonLabelProperty());

        // Bind time display (MM:SS format)
        pomodoroService.secondsRemainingProperty().addListener((obs, oldVal, newVal) -> {
            refreshTimeLabel(newVal.intValue());
        });

        // Initialize time display
        refreshTimeLabel(pomodoroService.secondsRemainingProperty().get());

        // Setup phase complete callback for notifications
        pomodoroService.setOnPhaseComplete(() -> {
            // Could trigger sound or notification here
            System.out.println("Phase complete! " + phaseLabel.getText() + " - " + sessionLabel.getText());
        });
    }

    /**
     * Create and add a circular progress ring to the timer display.
     */
    private void setupProgressRing() {
        progressRing = new Arc();
        progressRing.setCenterX(80);
        progressRing.setCenterY(80);
        progressRing.setRadiusX(80);
        progressRing.setRadiusY(80);
        progressRing.setStrokeWidth(3);
        progressRing.setStroke(Color.web("#4a7c8c"));
        progressRing.setFill(null);
        progressRing.setType(javafx.scene.shape.ArcType.OPEN);

        // Add behind time label for visual effect
        timerContainer.getChildren().add(0, progressRing);
        updateRingGeometry();
    }

    private void updateRingGeometry() {
        if (progressRing == null) {
            return;
        }

        double textWidth = Math.max(1, timeLabel.getLayoutBounds().getWidth());
        double textHeight = Math.max(1, timeLabel.getLayoutBounds().getHeight());
        double radius = Math.max(textWidth, textHeight) / 2.0 + RING_PADDING;

        progressRing.setCenterX(radius);
        progressRing.setCenterY(radius);
        progressRing.setRadiusX(radius);
        progressRing.setRadiusY(radius);
    }

    /**
     * Update the progress ring arc length based on time remaining.
     */
    private void updateProgressRing(int secondsRemaining) {
        if (progressRing == null) {
            return;
        }
        int totalSeconds = Math.max(1, pomodoroService.getCurrentPhaseTotalSeconds());
        double progress = (double) secondsRemaining / totalSeconds;
        progressRing.setLength(360 * progress);
    }

    private void refreshTimeLabel(int secondsRemaining) {
        int minutes = secondsRemaining / 60;
        int secs = secondsRemaining % 60;
        timeLabel.setText(String.format("%02d:%02d", minutes, secs));
        updateProgressRing(secondsRemaining);
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/timer_settings.fxml"));
            Parent root = loader.load();

            TimerSettingsController controller = loader.getController();
            controller.setTimerController(this);
            controller.loadInitialValues(
                    pomodoroService.getFocusMinutes(),
                    pomodoroService.getShortBreakMinutes(),
                    pomodoroService.getLongBreakMinutes(),
                    pomodoroService.getCyclesBeforeLongBreak());

            Stage stage = new Stage();
            stage.initOwner(settingsBtn.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Pomodoro Settings");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyTimerSettings(int focusMin, int shortBreakMin, int longBreakMin, int cycles) {
        pomodoroService.updateDurations(focusMin, shortBreakMin, longBreakMin, cycles);
    }
}
