package com.focusdesk.model;

public class Note {
    private final int id;
    private final int userId;
    private final String content;
    private final String createdAt;

    public Note(int id, int userId, String content, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public int getId()          { return id; }
    public int getUserId()      { return userId; }
    public String getContent()  { return content; }
    public String getCreatedAt(){ return createdAt; }
}
