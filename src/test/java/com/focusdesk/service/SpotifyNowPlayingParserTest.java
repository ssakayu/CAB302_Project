package com.focusdesk.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyNowPlayingParserTest {

    @Test
    void parse_extracts_title_artist_album_cover_progress() {
        String json = """
        {
          "is_playing": true,
          "progress_ms": 41000,
          "item": {
            "name": "Whatever It Takes",
            "duration_ms": 201000,
            "artists": [ { "name": "Imagine Dragons" } ],
            "album": {
              "name": "Evolve",
              "images": [ { "url": "https://example.com/cover.jpg" } ]
            }
          }
        }
        """;

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        SpotifyNowPlayingInfo info = SpotifyNowPlayingParser.parse(obj);

        assertNotNull(info);
        assertTrue(info.isPlaying());
        assertEquals(41000, info.progressMs());
        assertEquals(201000, info.durationMs());
        assertEquals("Whatever It Takes", info.trackTitle());
        assertEquals("Imagine Dragons", info.artists());
        assertEquals("Evolve", info.album());
        assertEquals("https://example.com/cover.jpg", info.coverUrl());
    }

    @Test
    void parse_returns_null_when_item_missing() {
        JsonObject obj = JsonParser.parseString("{\"is_playing\":true}").getAsJsonObject();
        assertNull(SpotifyNowPlayingParser.parse(obj));
    }

    // ✅ NEW TDD test (this should FAIL first = RED)
    @Test
    void parse_picks_smallest_cover_image() {
        String json = """
        {
          "is_playing": true,
          "progress_ms": 1000,
          "item": {
            "name": "Song",
            "duration_ms": 200000,
            "artists": [ { "name": "Artist" } ],
            "album": {
              "name": "Album",
              "images": [
                { "url": "https://example.com/large.jpg" },
                { "url": "https://example.com/medium.jpg" },
                { "url": "https://example.com/small.jpg" }
              ]
            }
          }
        }
        """;

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        SpotifyNowPlayingInfo info = SpotifyNowPlayingParser.parse(obj);

        assertNotNull(info);
        // ✅ expect smallest cover (last image)
        assertEquals("https://example.com/small.jpg", info.coverUrl());
    }
}