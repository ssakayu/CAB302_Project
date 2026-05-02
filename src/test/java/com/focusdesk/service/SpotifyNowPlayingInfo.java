package com.focusdesk.service;

public record SpotifyNowPlayingInfo(
        boolean isPlaying,
        long progressMs,
        long durationMs,
        String trackTitle,
        String artists,
        String album,
        String coverUrl
) {}