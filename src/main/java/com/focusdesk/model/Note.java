package com.focusdesk.model;

public class Note {
    private final int id;
    private final int userId;
    private final String content;

    public Note(int id, int userId, String content) {
        this.id = id;
        this.userId = userId;
        this.content = content;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getContent() { return content; }
}