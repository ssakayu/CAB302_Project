package com.focusdesk.model;

public class Task {
    private final int id;
    private final int userId;
    private final String title;
    private final boolean done;
    private final String priority;

    public Task(int id, int userId, String title, boolean done, String priority) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.done = done;
        this.priority = priority;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    public String getPriority() { return priority; }
}