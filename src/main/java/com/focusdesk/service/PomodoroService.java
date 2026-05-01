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

    private static final int DEFAULT_FOCUS_MINUTES = 25;
    private static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
    private static final int DEFAULT_LONG_BREAK_MINUTES = 15;
    private static final int DEFAULT_CYCLES_BEFORE_LONG_BREAK = 4;

    private final PomodoroDAO pomodoroDAO = new PomodoroDAO();
    private final PreferenceDAO preferenceDAO = new PreferenceDAO();

    // User preferences (in seconds)
    private int focusSeconds = DEFAULT_FOCUS_MINUTES * 60;
    private int shortBreakSeconds = DEFAULT_SHORT_BREAK_MINUTES * 60;
    private int longBreakSeconds = DEFAULT_LONG_BREAK_MINUTES * 60;
    private int cyclesBeforeLongBreak = DEFAULT_CYCLES_BEFORE_LONG_BREAK;

    // Timer state
    private final IntegerProperty secondsRemaining = new SimpleIntegerProperty(focusSeconds);
    private StringProperty phaseLabel = new SimpleStringProperty("Focus 🎯");
    private StringProperty buttonLabel = new SimpleStringProperty("Start");
    private final StringProperty sessionLabel = new SimpleStringProperty("Session 1 of 4");

    private boolean isFocusPhase = true;
    private boolean isRunning = false;
    private AnimationTimer animationTimer;
    private int completedFocusSessions = 0;
    private long lastUpdate = 0;

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
            int userId = Session.get().getCurrentUser().getId();
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
        updateSessionLabel();
    }

    /**
     * Setup the countdown timer using AnimationTimer.
     */
    private void setupTimer() {
        animationTimer = new AnimationTimer() {
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
        lastUpdate = 0;

        if (isFocusPhase) {
            // Log completed focus session
            try {
                int userId = Session.get().getCurrentUser().getId();
                pomodoroDAO.logSession(userId, focusSeconds / 60);
            } catch (Exception e) {
                e.printStackTrace();
            }

            completedFocusSessions++;

            // Decide break type
            boolean isLongBreak = (completedFocusSessions % cyclesBeforeLongBreak) == 0;
            int breakDuration = isLongBreak ? longBreakSeconds : shortBreakSeconds;

            // Switch to break phase
            isFocusPhase = false;
            phaseLabel.set(isLongBreak ? "Long Break ☕" : "Short Break ☕");
            secondsRemaining.set(breakDuration);
            updateSessionLabel();
        } else {
            // Switch back to focus phase
            isFocusPhase = true;
            phaseLabel.set("Focus 🎯");
            secondsRemaining.set(focusSeconds);
            if (completedFocusSessions >= cyclesBeforeLongBreak) {
                completedFocusSessions = 0;
            }
            updateSessionLabel();
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
            lastUpdate = 0;
            animationTimer.start();
        } else {
            animationTimer.stop();
            lastUpdate = 0;
        }
    }

    public void reset() {
        isRunning = false;
        animationTimer.stop();
        lastUpdate = 0;
        buttonLabel.set("Start");
        isFocusPhase = true;
        phaseLabel.set("Focus 🎯");
        completedFocusSessions = 0;
        secondsRemaining.set(focusSeconds);
        updateSessionLabel();
    }

    public void skipPhase() {
        isRunning = false;
        animationTimer.stop();
        lastUpdate = 0;
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

    private void updateSessionLabel() {
        int sessionNumber = isFocusPhase ? completedFocusSessions + 1 : Math.max(1, completedFocusSessions);
        sessionLabel.set("Session " + sessionNumber + " of " + cyclesBeforeLongBreak);
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

    public StringProperty sessionLabelProperty() {
        return sessionLabel;
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

    public int getCurrentPhaseTotalSeconds() {
        if (isFocusPhase) {
            return focusSeconds;
        }
        return (completedFocusSessions % cyclesBeforeLongBreak == 0) ? longBreakSeconds : shortBreakSeconds;
    }

    public void setOnPhaseComplete(Runnable callback) {
        this.onPhaseComplete = callback;
    }
}
