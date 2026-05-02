package com.focusdesk.service;

public class MusicPlayerState {

    private boolean isPlaying;
    private long progressMs;
    private long durationMs;

    private String title = "—";
    private String artists = "—";
    private String album = "—";
    private String coverUrl = null;

    // Apply data from Spotify API (poll every 10–15s)
    public void apply(SpotifyNowPlayingInfo info) {
        if (info == null) {
            isPlaying = false;
            progressMs = 0;
            durationMs = 0;
            title = "(nothing playing)";
            artists = "—";
            album = "—";
            coverUrl = null;
            return;
        }
        isPlaying = info.isPlaying();
        progressMs = info.progressMs();
        durationMs = info.durationMs();
        title = info.trackTitle();
        artists = info.artists();
        album = info.album();
        coverUrl = info.coverUrl();
    }

    // Tick locally every 1 second (no API call)
    public void tick(long deltaMs) {
        if (!isPlaying) return;
        progressMs = Math.min(progressMs + deltaMs, durationMs);
    }

    public String playbackLabel() {
        return (isPlaying ? "Playing" : "Paused") + " • " + msToMinSec(progressMs);
    }

    public static String msToMinSec(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min + ":" + (sec < 10 ? "0" + sec : sec);
    }

    // getters (useful later to bind to UI)
    public boolean isPlaying() { return isPlaying; }
    public long progressMs() { return progressMs; }
    public long durationMs() { return durationMs; }
    public String title() { return title; }
    public String artists() { return artists; }
    public String album() { return album; }
    public String coverUrl() { return coverUrl; }
}