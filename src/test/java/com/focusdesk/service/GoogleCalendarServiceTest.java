package com.focusdesk.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleCalendarServiceTest {

    @Test
    void buildAuthorizationUrlIncludesCalendarOAuthParameters() {
        GoogleCalendarService service = new GoogleCalendarService();

        URI uri = URI.create(service.buildAuthorizationUrl());
        Map<String, String> params = queryParams(uri);

        assertEquals("https", uri.getScheme());
        assertEquals("accounts.google.com", uri.getHost());
        assertEquals("/o/oauth2/v2/auth", uri.getPath());
        assertEquals(env("GOOGLE_CLIENT_ID"), params.get("client_id"));
        assertEquals(expectedRedirectUri(), params.get("redirect_uri"));
        assertEquals("code", params.get("response_type"));
        assertEquals("https://www.googleapis.com/auth/calendar.readonly", params.get("scope"));
        assertEquals("offline", params.get("access_type"));
        assertEquals("consent", params.get("prompt"));
    }

    @Test
    void parseEventsReadsTimedWeekdayEventsFromGoogleResponse() throws Exception {
        GoogleCalendarService service = new GoogleCalendarService();
        LocalDate weekStart = LocalDate.of(2026, 4, 27);
        String json = """
                {
                  "kind": "calendar#events",
                  "items": [
                    {
                      "kind": "calendar#event",
                      "summary": "CAB302 workshop",
                      "start": { "dateTime": "2026-04-27T10:00:00+10:00" },
                      "end": { "dateTime": "2026-04-27T12:30:00+10:00" }
                    },
                    {
                      "kind": "calendar#event",
                      "summary": "Project sync",
                      "start": { "dateTime": "2026-04-29T19:15:00+10:00" },
                      "end": { "dateTime": "2026-04-29T20:15:00+10:00" }
                    }
                  ]
                }
                """;

        List<GoogleCalendarService.CalendarEvent> events = parseEvents(service, json, weekStart);

        assertEquals(2, events.size());
        assertEquals("CAB302 workshop", events.get(0).title());
        assertEquals(LocalDate.of(2026, 4, 27), events.get(0).date());
        assertEquals(0, events.get(0).dayIndex());
        assertEquals(10.0, events.get(0).startHour());
        assertEquals(2.5, events.get(0).durationHours());
        assertEquals("Project sync", events.get(1).title());
        assertEquals(2, events.get(1).dayIndex());
        assertEquals(19.25, events.get(1).startHour());
        assertEquals(1.0, events.get(1).durationHours());
    }

    @Test
    void parseEventsSkipsWeekendAllDayAndIncompleteEvents() throws Exception {
        GoogleCalendarService service = new GoogleCalendarService();
        LocalDate weekStart = LocalDate.of(2026, 4, 27);
        String json = """
                {
                  "items": [
                    {
                      "kind": "calendar#event",
                      "summary": "Weekend event",
                      "start": { "dateTime": "2026-05-02T09:00:00+10:00" },
                      "end": { "dateTime": "2026-05-02T10:00:00+10:00" }
                    },
                    {
                      "kind": "calendar#event",
                      "summary": "All day event",
                      "start": { "date": "2026-04-28" },
                      "end": { "date": "2026-04-29" }
                    },
                    {
                      "kind": "calendar#event",
                      "start": { "dateTime": "2026-04-28T09:00:00+10:00" },
                      "end": { "dateTime": "2026-04-28T10:00:00+10:00" }
                    }
                  ]
                }
                """;

        List<GoogleCalendarService.CalendarEvent> events = parseEvents(service, json, weekStart);

        assertTrue(events.isEmpty());
    }

    @Test
    void parseEventsUsesHalfHourMinimumDuration() throws Exception {
        GoogleCalendarService service = new GoogleCalendarService();
        LocalDate weekStart = LocalDate.of(2026, 4, 27);
        String json = """
                {
                  "items": [
                    {
                      "kind": "calendar#event",
                      "summary": "Quick check-in",
                      "start": { "dateTime": "2026-04-30T14:00:00+10:00" },
                      "end": { "dateTime": "2026-04-30T14:10:00+10:00" }
                    }
                  ]
                }
                """;

        List<GoogleCalendarService.CalendarEvent> events = parseEvents(service, json, weekStart);

        assertEquals(1, events.size());
        assertEquals(0.5, events.get(0).durationHours());
    }

    @SuppressWarnings("unchecked")
    private List<GoogleCalendarService.CalendarEvent> parseEvents(
            GoogleCalendarService service,
            String json,
            LocalDate weekStart
    ) throws Exception {
        Method method = GoogleCalendarService.class.getDeclaredMethod("parseEvents", String.class, LocalDate.class);
        method.setAccessible(true);
        return (List<GoogleCalendarService.CalendarEvent>) method.invoke(service, json, weekStart);
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private String expectedRedirectUri() {
        String value = env("GOOGLE_REDIRECT_URI");
        return value.isBlank() ? GoogleCalendarService.DEFAULT_REDIRECT_URI : value;
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
