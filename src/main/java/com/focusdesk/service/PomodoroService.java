package com.focusdesk.service;

import com.focusdesk.app.Session;
import com.focusdesk.dao.PomodoroDAO;
import com.focusdesk.dao.PreferenceDAO;
import com.focusdesk.model.Preference;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Manages Pomodoro timer state, countdown, and session logging.
 */
public class PomodoroService {

    private final PomodoroDAO pomodoroDAO = new PomodoroDAO();
    private final PreferenceDAO preferenceDAO = new PreferenceDAO();

    // User preferences (in seconds)
    private int focusSeconds = 25 * 60;
    private int shortBreakSeconds = 5 * 60;
    private int longBreakSeconds = 15 * 60;
    private int cyclesBeforeLongBreak = 4;

    // Timer state
    private IntegerProperty secondsRemaining = new SimpleIntegerProperty(focusSeconds);
    private StringProperty phaseLabel = new SimpleStringProperty("Focus 🎯");
    private StringProperty buttonLabel = new SimpleStringProperty("Start");

    private boolean isFocusPhase = true;
    private boolean isRunning = false;
    private AnimationTimer animationTimer;
    private int completedCycles = 0;

    // Callback for when phase changes
    private Runnable onPhaseComplete;

    public PomodoroService() {
        loadPreferences();
        setupTimer();
    }

    /**
     * Load user's saved Pomodoro preferences from database.
     */
    private void loadPreferences() {
        try {
            int userId = Session.getCurrentUser().getId();
            Preference prefs = preferenceDAO.getByUserId(userId);
            if (prefs != null) {
                focusSeconds = prefs.getFocusMinutes() * 60;
                shortBreakSeconds = prefs.getShortBreakMinutes() * 60;
                longBreakSeconds = prefs.getLongBreakMinutes() * 60;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        secondsRemaining.set(focusSeconds);
    }

    /**
     * Setup the countdown timer using AnimationTimer.
     */
    private void setupTimer() {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (!isRunning) return;

                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                long elapsed = (now - lastUpdate) / 1_000_000_000L; // Convert nanoseconds to seconds
                if (elapsed >= 1) {
                    lastUpdate = now;
                    int current = secondsRemaining.get();
                    if (current > 0) {
                        secondsRemaining.set(current - 1);
                    } else {
                        onTimerComplete();
                    }
                }
            }
        };
    }

    /**
     * Called when timer reaches zero.
     */
    private void onTimerComplete() {
        isRunning = false;
        buttonLabel.set("Start");

        if (isFocusPhase) {
            // Log completed focus session
            try {
                int userId = Session.getCurrentUser().getId();
                pomodoroDAO.logSession(userId, focusSeconds / 60);
            } catch (Exception e) {
                e.printStackTrace();
            }

            completedCycles++;

            // Decide break type
            boolean isLongBreak = (completedCycles % cyclesBeforeLongBreak) == 0;
            int breakDuration = isLongBreak ? longBreakSeconds : shortBreakSeconds;

            // Switch to break phase
            isFocusPhase = false;
            phaseLabel.set(isLongBreak ? "Long Break ☕" : "Short Break ☕");
            secondsRemaining.set(breakDuration);
        } else {
            // Switch back to focus phase
            isFocusPhase = true;
            phaseLabel.set("Focus 🎯");
            secondsRemaining.set(focusSeconds);
        }

        if (onPhaseComplete != null) {
            onPhaseComplete.run();
        }
    }

    // -------------------------------------------------------------------------
    // Control Methods
    // -------------------------------------------------------------------------

    public void startPause() {
        isRunning = !isRunning;
        buttonLabel.set(isRunning ? "Pause" : "Start");
        if (isRunning) {
            animationTimer.start();
        } else {
            animationTimer.stop();
        }
    }

    public void reset() {
        isRunning = false;
        animationTimer.stop();
        buttonLabel.set("Start");
        isFocusPhase = true;
        phaseLabel.set("Focus 🎯");
        completedCycles = 0;
        secondsRemaining.set(focusSeconds);
    }

    public void skipPhase() {
        isRunning = false;
        animationTimer.stop();
        secondsRemaining.set(0);
        onTimerComplete();
    }

    public void updateDurations(int focusMin, int shortBreakMin, int longBreakMin, int cycles) {
        this.focusSeconds = focusMin * 60;
        this.shortBreakSeconds = shortBreakMin * 60;
        this.longBreakSeconds = longBreakMin * 60;
        this.cyclesBeforeLongBreak = cycles;
        reset();
    }

    // -------------------------------------------------------------------------
    // Getters & Properties
    // -------------------------------------------------------------------------

    public IntegerProperty secondsRemainingProperty() {
        return secondsRemaining;
    }

    public StringProperty phaseLabelProperty() {
        return phaseLabel;
    }

    public StringProperty buttonLabelProperty() {
        return buttonLabel;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getFocusMinutes() {
        return focusSeconds / 60;
    }

    public int getShortBreakMinutes() {
        return shortBreakSeconds / 60;
    }

    public int getLongBreakMinutes() {
        return longBreakSeconds / 60;
    }

    public int getCyclesBeforeLongBreak() {
        return cyclesBeforeLongBreak;
    }

    public void setOnPhaseComplete(Runnable callback) {
        this.onPhaseComplete = callback;
    }
}
