package com.focusdesk.model;

public class Preference {
    private final int userId;
    private final String theme;
    private final String enabledWidgets;
    private final int focusMinutes;
    private final int shortBreakMinutes;
    private final int longBreakMinutes;
    private final int sessionsBeforeLongBreak;
    private final boolean enableSoundNotifications;
    private final double widgetX;
    private final double widgetY;
    private final double widgetOpacity;

    public Preference(int userId, String theme, String enabledWidgets,
                      int focusMinutes, int shortBreakMinutes, int longBreakMinutes,
                      int sessionsBeforeLongBreak, boolean enableSoundNotifications,
                      double widgetX, double widgetY, double widgetOpacity) {
        this.userId = userId;
        this.theme = theme;
        this.enabledWidgets = enabledWidgets;
        this.focusMinutes = focusMinutes;
        this.shortBreakMinutes = shortBreakMinutes;
        this.longBreakMinutes = longBreakMinutes;
        this.sessionsBeforeLongBreak = sessionsBeforeLongBreak;
        this.enableSoundNotifications = enableSoundNotifications;
        this.widgetX = widgetX;
        this.widgetY = widgetY;
        this.widgetOpacity = widgetOpacity;
    }

    public int getUserId() { return userId; }
    public String getTheme() { return theme; }
    public String getEnabledWidgets() { return enabledWidgets; }
    public int getFocusMinutes() { return focusMinutes; }
    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public int getLongBreakMinutes() { return longBreakMinutes; }
    public int getSessionsBeforeLongBreak() { return sessionsBeforeLongBreak; }
    public boolean isEnableSoundNotifications() { return enableSoundNotifications; }
    public double getWidgetX() { return widgetX; }
    public double getWidgetY() { return widgetY; }
    public double getWidgetOpacity() { return widgetOpacity; }
}