package com.focusdesk.controller.widget;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.model.PomodoroSettings;
import com.focusdesk.util.TaskRunner;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.util.Objects;

public class TimerSlideController {

	private static final int DEFAULT_FOCUS_MINUTES = 25;
	private static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
	private static final int DEFAULT_LONG_BREAK_MINUTES = 15;
	private static final int DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4;

	@FXML private VBox timerRoot;
	@FXML private Label phaseLabel;
	@FXML private Label sessionLabel;
	@FXML private Label timerLabel;
	@FXML private Circle timerRing;
	@FXML private Button startPauseButton;
	@FXML private Button resetButton;
	@FXML private Button skipButton;
	@FXML private Button settingsButton;

	private final PreferenceDAO preferenceDAO = new PreferenceDAO();
	private final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> onTick()));

	private PomodoroSettings settings = defaultSettings();
	private Phase phase = Phase.FOCUS;
	private boolean running;
	private int completedFocusSessions;
	private int remainingSeconds;

	private enum Phase {
		FOCUS("Focus", "timer-ring-focus"),
		SHORT_BREAK("Short Break", "timer-ring-short-break"),
		LONG_BREAK("Long Break", "timer-ring-long-break");

		private final String label;
		private final String styleClass;

		Phase(String label, String styleClass) {
			this.label = label;
			this.styleClass = styleClass;
		}

		String label() {
			return label;
		}

		String styleClass() {
			return styleClass;
		}
	}

	@FXML
	private void initialize() {
		timeline.setCycleCount(Animation.INDEFINITE);
		phase = Phase.FOCUS;
		completedFocusSessions = 0;
		running = false;
		loadSettings();
	}

	@FXML
	private void onStartPause() {
		if (running) {
			pauseTimer();
		} else {
			startTimer();
		}
	}

	@FXML
	private void onReset() {
		resetCurrentPhase(false);
	}

	@FXML
	private void onSkip() {
		advancePhase(running);
	}

	@FXML
	private void onOpenSettings() {
		Dialog<PomodoroSettings> dialog = new Dialog<>();
		dialog.setTitle("Pomodoro Settings");
		dialog.initOwner(timerRoot.getScene().getWindow());
		dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

		ButtonType cancelType = ButtonType.CANCEL;
		ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().setAll(cancelType, saveType);

		Spinner<Integer> focusSpinner = createSpinner(settings.getFocusMinutes(), 1, 180);
		Spinner<Integer> shortSpinner = createSpinner(settings.getShortBreakMinutes(), 1, 120);
		Spinner<Integer> longSpinner = createSpinner(settings.getLongBreakMinutes(), 1, 240);
		Spinner<Integer> sessionsSpinner = createSpinner(settings.getSessionsBeforeLongBreak(), 1, 12);
		CheckBox soundCheck = new CheckBox("Enable sound notifications");
		soundCheck.setSelected(settings.isSoundNotifications());

		GridPane grid = new GridPane();
		grid.getStyleClass().add("pomodoro-settings-grid");
		grid.setHgap(12);
		grid.setVgap(12);
		grid.setAlignment(Pos.CENTER_LEFT);
		grid.getColumnConstraints().addAll(
				new ColumnConstraints(160),
				new ColumnConstraints(120, 140, Double.MAX_VALUE));

		addSettingRow(grid, 0, "Focus Duration (min):", focusSpinner);
		addSettingRow(grid, 1, "Short Break (min):", shortSpinner);
		addSettingRow(grid, 2, "Long Break (min):", longSpinner);
		addSettingRow(grid, 3, "Sessions Before Long Break:", sessionsSpinner);

		VBox content = new VBox(14);
		content.getStyleClass().add("pomodoro-settings-content");
		content.getChildren().addAll(
				titledLabel("Pomodoro Settings", "pomodoro-settings-title"),
				grid,
				soundCheck);

		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().setPrefWidth(360);
		dialog.getDialogPane().getStyleClass().add("pomodoro-settings-dialog");
		dialog.getDialogPane().getStylesheets().add(
				Objects.requireNonNull(getClass().getResource("/css/widget.css")).toExternalForm());

		dialog.setResultConverter(button -> {
			if (button == saveType) {
				return new PomodoroSettings(
						focusSpinner.getValue(),
						shortSpinner.getValue(),
						longSpinner.getValue(),
						sessionsSpinner.getValue(),
						soundCheck.isSelected());
			}
			return null;
		});

		dialog.setOnShown(event -> styleDialogButtons(dialog.getDialogPane(), saveType, cancelType));

		dialog.showAndWait().ifPresent(this::applySettings);
	}

	private void loadSettings() {
		int userId = currentUserId();
		if (userId <= 0) {
			settings = defaultSettings();
			resetCurrentPhase(false);
			return;
		}

		TaskRunner.run(
				() -> preferenceDAO.getPomodoroSettings(userId),
				loaded -> {
					settings = loaded != null ? loaded : defaultSettings();
					resetCurrentPhase(false);
				},
				err -> {
					System.err.println("Failed to load pomodoro settings: " + err.getMessage());
					settings = defaultSettings();
					resetCurrentPhase(false);
				});
	}

	private void applySettings(PomodoroSettings newSettings) {
		settings = newSettings;
		int userId = currentUserId();
		if (userId > 0) {
			TaskRunner.run(
					() -> {
						preferenceDAO.savePomodoroSettings(userId, newSettings);
						return null;
					},
					ignored -> {
					},
					err -> System.err.println("Failed to save pomodoro settings: " + err.getMessage()));
		}

		resetCurrentPhase(running);
	}

	private void startTimer() {
		running = true;
		timeline.play();
		updateControls();
	}

	private void pauseTimer() {
		running = false;
		timeline.pause();
		updateControls();
	}

	private void resetCurrentPhase(boolean shouldRun) {
		remainingSeconds = durationFor(phase);
		running = shouldRun;
		if (running) {
			timeline.play();
		} else {
			timeline.pause();
		}
		updateView();
	}

	private void advancePhase(boolean keepRunning) {
		if (phase == Phase.FOCUS) {
			completedFocusSessions++;
			phase = (completedFocusSessions % settings.getSessionsBeforeLongBreak() == 0)
					? Phase.LONG_BREAK
					: Phase.SHORT_BREAK;
			notifyComplete();
		} else {
			phase = Phase.FOCUS;
			if (completedFocusSessions >= settings.getSessionsBeforeLongBreak()) {
				completedFocusSessions = 0;
			}
		}

		remainingSeconds = durationFor(phase);
		running = keepRunning;
		if (running) {
			timeline.play();
		} else {
			timeline.pause();
		}
		updateView();
	}

	private void onTick() {
		if (!running) {
			return;
		}

		remainingSeconds--;
		if (remainingSeconds <= 0) {
			advancePhase(true);
		} else {
			updateTimerDisplay();
		}
	}

	private void updateView() {
		phaseLabel.setText(phase.label());
		sessionLabel.setText("Session " + currentSessionNumber() + " of " + settings.getSessionsBeforeLongBreak());
		timerRing.getStyleClass().setAll("pomodoro-ring", phase.styleClass());
		updateTimerDisplay();
		updateControls();
	}

	private void updateTimerDisplay() {
		timerLabel.setText(formatTime(remainingSeconds));
	}

	private void updateControls() {
		startPauseButton.setText(running ? "Pause" : "Start");
		resetButton.setDisable(remainingSeconds <= 0);
	}

	private int durationFor(Phase currentPhase) {
		return switch (currentPhase) {
			case FOCUS -> settings.getFocusMinutes() * 60;
			case SHORT_BREAK -> settings.getShortBreakMinutes() * 60;
			case LONG_BREAK -> settings.getLongBreakMinutes() * 60;
		};
	}

	private int currentSessionNumber() {
		return Math.min(completedFocusSessions + 1, settings.getSessionsBeforeLongBreak());
	}

	private String formatTime(int totalSeconds) {
		int safeSeconds = Math.max(0, totalSeconds);
		int minutes = safeSeconds / 60;
		int seconds = safeSeconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	private void notifyComplete() {
		if (!settings.isSoundNotifications()) {
			return;
		}

		try {
			Toolkit.getDefaultToolkit().beep();
		} catch (Exception ignored) {
		}
	}

	private Spinner<Integer> createSpinner(int value, int min, int max) {
		Spinner<Integer> spinner = new Spinner<>();
		spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value));
		spinner.setEditable(true);
		spinner.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(spinner, Priority.ALWAYS);
		return spinner;
	}

	private void addSettingRow(GridPane grid, int row, String labelText, Node control) {
		Label label = new Label(labelText);
		label.getStyleClass().add("pomodoro-settings-label");
		grid.add(label, 0, row);
		grid.add(control, 1, row);
	}

	private Label titledLabel(String text, String styleClass) {
		Label label = new Label(text);
		label.getStyleClass().add(styleClass);
		return label;
	}

	private void styleDialogButtons(DialogPane pane, ButtonType saveType, ButtonType cancelType) {
		Node saveButtonNode = pane.lookupButton(saveType);
		Node cancelButtonNode = pane.lookupButton(cancelType);
		if (saveButtonNode instanceof Button saveButton) {
			saveButton.getStyleClass().add("timer-dialog-save-btn");
		}
		if (cancelButtonNode instanceof Button cancelButton) {
			cancelButton.getStyleClass().add("timer-dialog-cancel-btn");
		}
	}

	private int currentUserId() {
		return Session.get().getCurrentUser() == null ? -1 : Session.get().getCurrentUser().getId();
	}

	private PomodoroSettings defaultSettings() {
		return new PomodoroSettings(
				DEFAULT_FOCUS_MINUTES,
				DEFAULT_SHORT_BREAK_MINUTES,
				DEFAULT_LONG_BREAK_MINUTES,
				DEFAULT_SESSIONS_BEFORE_LONG_BREAK,
				true);
	}
}
