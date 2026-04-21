package com.focusdesk.model;

public class PomodoroSession {
    private final int id;
    private final int userId;
    private final int focusMinutes;

    public PomodoroSession(int id, int userId, int focusMinutes) {
        this.id = id;
        this.userId = userId;
        this.focusMinutes = focusMinutes;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getFocusMinutes() { return focusMinutes; }
}