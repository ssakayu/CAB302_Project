package com.focusdesk.app;

import com.focusdesk.model.User;

public final class Session {
    private static User currentUser;

    private Session() {}

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User u) { currentUser = u; }
    public static void clear() { currentUser = null; }
}