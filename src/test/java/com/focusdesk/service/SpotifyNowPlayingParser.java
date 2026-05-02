package com.focusdesk.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class SpotifyNowPlayingParser {

    private SpotifyNowPlayingParser() {}

    public static SpotifyNowPlayingInfo parse(JsonObject playing) {
        if (playing == null) return null;
        if (!playing.has("item") || playing.get("item").isJsonNull()) return null;

        boolean isPlaying = playing.has("is_playing") && playing.get("is_playing").getAsBoolean();
        long progressMs = playing.has("progress_ms") && !playing.get("progress_ms").isJsonNull()
                ? playing.get("progress_ms").getAsLong()
                : 0;

        JsonObject item = playing.getAsJsonObject("item");

        String title = item.has("name") ? item.get("name").getAsString() : "(unknown)";
        long durationMs = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0;

        // Artists
        String artists = "—";
        if (item.has("artists")) {
            JsonArray arr = item.getAsJsonArray("artists");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr.get(i).getAsJsonObject().get("name").getAsString());
            }
            artists = sb.toString();
        }

        // Album + cover
        String album = "—";
        String coverUrl = null;

        if (item.has("album") && !item.get("album").isJsonNull()) {
            JsonObject albumObj = item.getAsJsonObject("album");
            if (albumObj.has("name")) album = albumObj.get("name").getAsString();

            JsonArray images = albumObj.getAsJsonArray("images");
            coverUrl = images.get(0).getAsJsonObject().get("url").getAsString();
        }

        return new SpotifyNowPlayingInfo(
                isPlaying,
                progressMs,
                durationMs,
                title,
                artists,
                album,
                coverUrl
        );
    }
}