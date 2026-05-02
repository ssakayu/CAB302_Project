package com.focusdesk.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class SpotifyNowPlayingParser {

    private SpotifyNowPlayingParser() {}

    public static SpotifyNowPlayingInfo parse(JsonObject playing) {
        if (playing == null) return null;
        if (!playing.has("item") || playing.get("item").isJsonNull()) return null;

        boolean isPlaying = getBoolean(playing, "is_playing", false);
        long progressMs = getLong(playing, "progress_ms", 0);

        JsonObject item = playing.getAsJsonObject("item");
        if (item == null) return null;

        String title = getString(item, "name", "(unknown)");
        long durationMs = getLong(item, "duration_ms", 0);

        String artists = joinArtistNames(item);
        AlbumInfo albumInfo = readAlbumInfo(item);

        return new SpotifyNowPlayingInfo(
                isPlaying,
                progressMs,
                durationMs,
                title,
                artists,
                albumInfo.albumName,
                albumInfo.coverUrl
        );
    }

    // ---------------- helper methods ----------------

    private static String joinArtistNames(JsonObject item) {
        if (!item.has("artists") || item.get("artists").isJsonNull()) return "—";

        JsonArray arr = item.getAsJsonArray("artists");
        if (arr == null || arr.size() == 0) return "—";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(", ");
            JsonObject a = arr.get(i).getAsJsonObject();
            sb.append(getString(a, "name", "—"));
        }
        return sb.toString();
    }

    private static AlbumInfo readAlbumInfo(JsonObject item) {
        if (!item.has("album") || item.get("album").isJsonNull()) return new AlbumInfo("—", null);

        JsonObject albumObj = item.getAsJsonObject("album");
        String albumName = getString(albumObj, "name", "—");

        String coverUrl = null;
        if (albumObj.has("images") && !albumObj.get("images").isJsonNull()) {
            JsonArray images = albumObj.getAsJsonArray("images");
            coverUrl = pickSmallestCoverUrl(images); // keeps your “smallest image” test passing
        }

        return new AlbumInfo(albumName, coverUrl);
    }

    // ✅ smallest = last image (matches your TDD test)
    private static String pickSmallestCoverUrl(JsonArray images) {
        if (images == null || images.size() == 0) return null;
        JsonObject img = images.get(images.size() - 1).getAsJsonObject();
        return getString(img, "url", null);
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private static long getLong(JsonObject obj, String key, long fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsLong();
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsBoolean();
    }

    private record AlbumInfo(String albumName, String coverUrl) {}
}