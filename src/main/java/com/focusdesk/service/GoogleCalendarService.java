package com.focusdesk.service;

import com.focusdesk.dao.OAuthTokenDAO;
import com.focusdesk.model.OAuthToken;
import com.focusdesk.util.HttpUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleCalendarService {

    public static final String PROVIDER = "google_calendar";
    public static final int CALLBACK_PORT = 8765;
    public static final String CALLBACK_PATH = "/oauth2/callback";
    public static final String DEFAULT_REDIRECT_URI = "http://127.0.0.1:" + CALLBACK_PORT + CALLBACK_PATH;
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refresh_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TOKEN_TYPE_PATTERN = Pattern.compile("\"token_type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRES_IN_PATTERN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("\"scope\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EVENT_OBJECT_PATTERN = Pattern.compile("\\{\\s*\"kind\"\\s*:\\s*\"calendar#event\"");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\"summary\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("\"dateTime\"\\s*:\\s*\"([^\"]+)\"");

    private final OAuthTokenDAO tokenDAO = new OAuthTokenDAO();

    public boolean isConfigured() {
        return !clientId().isBlank() && !clientSecret().isBlank();
    }

    public boolean isConnected(int userId) throws Exception {
        return tokenDAO.find(userId, PROVIDER) != null;
    }

    public String buildAuthorizationUrl() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", clientId());
        params.put("redirect_uri", redirectUri());
        params.put("response_type", "code");
        params.put("scope", "https://www.googleapis.com/auth/calendar.readonly");
        params.put("access_type", "offline");
        params.put("prompt", "consent");
        StringBuilder url = new StringBuilder(AUTH_URL).append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                url.append("&");
            }
            first = false;
            url.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return url.toString();
    }

    public void exchangeCodeAndStore(int userId, String code) throws Exception {
        String body = HttpUtil.postForm(TOKEN_URL, Map.of(
                "client_id", clientId(),
                "client_secret", clientSecret(),
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", redirectUri()
        ), Map.of());
        tokenDAO.upsert(toToken(userId, body, null));
    }

    public List<CalendarEvent> listWeekEvents(int userId, LocalDate weekStart) throws Exception {
        OAuthToken token = requireValidToken(userId);
        String timeMin = weekStart.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String timeMax = weekStart.plusDays(5).atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String url = EVENTS_URL
                + "?singleEvents=true&orderBy=startTime"
                + "&timeMin=" + encode(timeMin)
                + "&timeMax=" + encode(timeMax);

        String body = HttpUtil.get(url, Map.of("Authorization", "Bearer " + token.accessToken()));
        return parseEvents(body, weekStart);
    }

    private OAuthToken requireValidToken(int userId) throws Exception {
        OAuthToken token = tokenDAO.find(userId, PROVIDER);
        if (token == null) {
            throw new IllegalStateException("Google Calendar is not connected.");
        }
        if (token.expiresAt() != null && !token.expiresAt().isBlank()) {
            OffsetDateTime expiresAt = OffsetDateTime.parse(token.expiresAt());
            if (expiresAt.isBefore(OffsetDateTime.now().plusMinutes(1))) {
                return refreshToken(token);
            }
        }
        return token;
    }

    private OAuthToken refreshToken(OAuthToken oldToken) throws Exception {
        if (oldToken.refreshToken() == null || oldToken.refreshToken().isBlank()) {
            return oldToken;
        }
        String body = HttpUtil.postForm(TOKEN_URL, Map.of(
                "client_id", clientId(),
                "client_secret", clientSecret(),
                "refresh_token", oldToken.refreshToken(),
                "grant_type", "refresh_token"
        ), Map.of());
        OAuthToken refreshed = toToken(oldToken.userId(), body, oldToken.refreshToken());
        tokenDAO.upsert(refreshed);
        return refreshed;
    }

    private OAuthToken toToken(int userId, String json, String fallbackRefreshToken) {
        String accessToken = capture(json, ACCESS_TOKEN_PATTERN);
        String refreshToken = capture(json, REFRESH_TOKEN_PATTERN);
        String tokenType = capture(json, TOKEN_TYPE_PATTERN);
        String scope = capture(json, SCOPE_PATTERN);
        String expiresIn = capture(json, EXPIRES_IN_PATTERN);
        String expiresAt = null;
        if (expiresIn != null) {
            expiresAt = OffsetDateTime.now().plusSeconds(Long.parseLong(expiresIn)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return new OAuthToken(
                userId,
                PROVIDER,
                accessToken,
                (refreshToken == null || refreshToken.isBlank()) ? fallbackRefreshToken : refreshToken,
                tokenType,
                scope,
                expiresAt
        );
    }

    List<CalendarEvent> parseEvents(String json, LocalDate weekStart) {
        List<CalendarEvent> parsed = new ArrayList<>();
        for (String item : eventObjects(json)) {
            String summary = capture(item, SUMMARY_PATTERN);
            List<String> dateTimes = captureAll(item, DATE_TIME_PATTERN);
            if (summary == null || dateTimes.size() < 2) {
                continue;
            }
            OffsetDateTime start = OffsetDateTime.parse(dateTimes.get(0));
            OffsetDateTime end = OffsetDateTime.parse(dateTimes.get(1));
            LocalDate localDate = start.toLocalDate();
            int dayIndex = (int) ChronoUnit.DAYS.between(weekStart, localDate);
            if (dayIndex < 0 || dayIndex > 4) {
                continue;
            }
            double startHour = start.getHour() + (start.getMinute() / 60.0);
            double duration = Math.max(0.5, (end.toEpochSecond() - start.toEpochSecond()) / 3600.0);
            parsed.add(new CalendarEvent(summary, localDate, dayIndex, startHour, duration));
        }
        return parsed;
    }

    private List<String> eventObjects(String json) {
        List<String> events = new ArrayList<>();
        List<Integer> objectStarts = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                objectStarts.add(i);
            } else if (current == '}' && !objectStarts.isEmpty()) {
                int start = objectStarts.remove(objectStarts.size() - 1);
                String candidate = json.substring(start, i + 1);
                if (isCalendarEventObject(candidate)) {
                    events.add(candidate);
                }
            }
        }

        return events;
    }

    private boolean isCalendarEventObject(String object) {
        return EVENT_OBJECT_PATTERN.matcher(object).lookingAt();
    }

    private List<String> captureAll(String source, Pattern pattern) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            values.add(unescape(matcher.group(1)));
        }
        return values;
    }

    private String capture(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        return unescape(matcher.group(1));
    }

    private String unescape(String raw) {
        return raw.replace("\\\"", "\"").replace("\\n", "\n");
    }

    private String clientId() {
        return env("GOOGLE_CLIENT_ID");
    }

    private String clientSecret() {
        return env("GOOGLE_CLIENT_SECRET");
    }

    private String redirectUri() {
        String value = env("GOOGLE_REDIRECT_URI");
        return value.isBlank() ? DEFAULT_REDIRECT_URI : value;
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record CalendarEvent(String title, LocalDate date, int dayIndex, double startHour, double durationHours) {}
}
