package com.focusdesk.model;

public class PomodoroSettings {
    private int focusMinutes;
    private int shortBreakMinutes;
    private int longBreakMinutes;
    private int sessionsBeforeLongBreak;
    private boolean soundNotifications;

    public PomodoroSettings(int focusMinutes, int shortBreakMinutes, int longBreakMinutes,
                            int sessionsBeforeLongBreak, boolean soundNotifications) {
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.sessionsBeforeLongBreak = sessionsBeforeLongBreak;
        this.soundNotifications = soundNotifications;
    }

    public int getFocusMinutes() {
        return focusMinutes;
    }

    public void setFocusMinutes(int focusMinutes) {
        this.focusMinutes = focusMinutes;
    }

    public int getShortBreakMinutes() {
        return shortBreakMinutes;
    }

    public void setShortBreakMinutes(int shortBreakMinutes) {
        this.shortBreakMinutes = shortBreakMinutes;
    }

    public int getLongBreakMinutes() {
        return longBreakMinutes;
    }

    public void setLongBreakMinutes(int longBreakMinutes) {
        this.longBreakMinutes = longBreakMinutes;
    }

    public int getSessionsBeforeLongBreak() {
        return sessionsBeforeLongBreak;
    }

    public void setSessionsBeforeLongBreak(int sessionsBeforeLongBreak) {
        this.sessionsBeforeLongBreak = sessionsBeforeLongBreak;
    }

    public boolean isSoundNotifications() {
        return soundNotifications;
    }

    public void setSoundNotifications(boolean soundNotifications) {
        this.soundNotifications = soundNotifications;
    }
}