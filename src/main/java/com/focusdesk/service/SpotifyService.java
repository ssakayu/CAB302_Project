package com.focusdesk.service;

import com.focusdesk.dao.OAuthTokenDAO;
import com.focusdesk.model.OAuthToken;
import com.focusdesk.util.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SpotifyService {

    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    // ✅ Add modify playback scope for play/pause/next/prev/seek (Premium required)
    private static final String SCOPES = String.join(" ",
            "user-read-email",
            "user-read-private",
            "user-read-playback-state",
            "user-read-currently-playing",
            "user-modify-playback-state"
    );

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    private static String lastVerifier;
    private static String lastState;

    private static String getClientIdOrThrow() {
        String clientId = AppConfig.getSpotifyClientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Spotify Client ID not set. Set it first.");
        }
        return clientId;
    }

    public void connect(int userId) throws Exception {
        String clientId = getClientIdOrThrow();

        lastVerifier = randomBase64Url(64);
        String challenge = sha256Base64Url(lastVerifier);
        lastState = randomBase64Url(16);

        String url = AUTH_URL +
                "?response_type=code" +
                "&client_id=" + enc(clientId) +
                "&scope=" + enc(SCOPES) +
                "&redirect_uri=" + enc(REDIRECT_URI) +
                "&state=" + enc(lastState) +
                "&code_challenge_method=S256" +
                "&code_challenge=" + enc(challenge);

        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer server = startCallbackServer(codeFuture);

        Desktop.getDesktop().browse(new URI(url));

        String code = codeFuture.join();
        server.stop(0);

        JsonObject tokenJson = exchangeCodeForToken(code, lastVerifier, clientId);

        String accessToken = tokenJson.get("access_token").getAsString();
        String refreshToken = tokenJson.has("refresh_token") ? tokenJson.get("refresh_token").getAsString() : null;
        String tokenType = tokenJson.has("token_type") ? tokenJson.get("token_type").getAsString() : "Bearer";
        String scope = tokenJson.has("scope") ? tokenJson.get("scope").getAsString() : null;

        long expiresIn = tokenJson.get("expires_in").getAsLong();
        long expiresAt = Instant.now().getEpochSecond() + expiresIn;

        OAuthToken token = new OAuthToken(
                userId,
                "spotify",
                accessToken,
                refreshToken,
                tokenType,
                scope,
                String.valueOf(expiresAt)
        );

        new OAuthTokenDAO().upsert(token);
    }

    public JsonObject getMe(int userId) throws Exception {
        String access = getValidAccessToken(userId);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me"))
                .header("Authorization", "Bearer " + access)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("/v1/me failed: " + res.statusCode() + " " + res.body());

        return gson.fromJson(res.body(), JsonObject.class);
    }

    public JsonObject getCurrentlyPlaying(int userId) throws Exception {
        String access = getValidAccessToken(userId);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                .header("Authorization", "Bearer " + access)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 204) return null;
        if (res.statusCode() != 200)
            throw new RuntimeException("currently-playing failed: " + res.statusCode() + " " + res.body());

        return gson.fromJson(res.body(), JsonObject.class);
    }

    // ----------------------------
    // Playback Controls
    // ----------------------------

    public void play(int userId) throws Exception {
        String access = getValidAccessToken(userId);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/play"))
                .header("Authorization", "Bearer " + access)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        handleControlResponse(http.send(req, HttpResponse.BodyHandlers.ofString()), "play");
    }

    public void pause(int userId) throws Exception {
        String access = getValidAccessToken(userId);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/pause"))
                .header("Authorization", "Bearer " + access)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        handleControlResponse(http.send(req, HttpResponse.BodyHandlers.ofString()), "pause");
    }

    public void nextTrack(int userId) throws Exception {
        String access = getValidAccessToken(userId);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/next"))
                .header("Authorization", "Bearer " + access)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        handleControlResponse(http.send(req, HttpResponse.BodyHandlers.ofString()), "next");
    }

    public void previousTrack(int userId) throws Exception {
        String access = getValidAccessToken(userId);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/previous"))
                .header("Authorization", "Bearer " + access)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        handleControlResponse(http.send(req, HttpResponse.BodyHandlers.ofString()), "previous");
    }

    public void seekTo(int userId, long positionMs) throws Exception {
        String access = getValidAccessToken(userId);
        String url = "https://api.spotify.com/v1/me/player/seek?position_ms=" + positionMs;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + access)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        handleControlResponse(http.send(req, HttpResponse.BodyHandlers.ofString()), "seek");
    }

    private void handleControlResponse(HttpResponse<String> res, String action) {
        int code = res.statusCode();
        if (code == 204 || code == 200) return;

        if (code == 403) {
            throw new RuntimeException("Spotify " + action + " blocked (need Premium + user-modify-playback-state).");
        }
        if (code == 404) {
            throw new RuntimeException("No active Spotify device. Open Spotify and play a song first.");
        }
        throw new RuntimeException("Spotify " + action + " failed: " + code + " " + res.body());
    }

    // ----------------------------
    // Token handling
    // ----------------------------

    private String getValidAccessToken(int userId) throws Exception {
        OAuthTokenDAO dao = new OAuthTokenDAO();
        OAuthToken token = dao.find(userId, "spotify");
        if (token == null) throw new IllegalStateException("Spotify not connected for this user.");

        if (!isExpiredSoon(token)) return token.accessToken();
        if (token.refreshToken() == null) throw new IllegalStateException("No refresh token saved.");

        String clientId = getClientIdOrThrow();
        JsonObject refreshed = refreshAccessToken(token.refreshToken(), clientId);

        String newAccess = refreshed.get("access_token").getAsString();
        long expiresIn = refreshed.get("expires_in").getAsLong();
        long newExpiresAt = Instant.now().getEpochSecond() + expiresIn;

        String tokenType = refreshed.has("token_type") ? refreshed.get("token_type").getAsString() : token.tokenType();
        String scope = refreshed.has("scope") ? refreshed.get("scope").getAsString() : token.scope();

        OAuthToken updated = new OAuthToken(
                userId,
                "spotify",
                newAccess,
                token.refreshToken(),
                tokenType,
                scope,
                String.valueOf(newExpiresAt)
        );

        dao.upsert(updated);
        return newAccess;
    }

    private boolean isExpiredSoon(OAuthToken token) {
        try {
            long exp = Long.parseLong(token.expiresAt());
            long now = System.currentTimeMillis() / 1000;
            return exp <= (now + 30);
        } catch (Exception e) {
            return true;
        }
    }

    // ----------------------------
    // OAuth HTTP
    // ----------------------------

    private static JsonObject exchangeCodeForToken(String code, String verifier, String clientId) throws Exception {
        String body = form(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "client_id", clientId,
                "code_verifier", verifier
        ));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("token exchange failed: " + res.statusCode() + " " + res.body());

        return gson.fromJson(res.body(), JsonObject.class);
    }

    private static JsonObject refreshAccessToken(String refreshToken, String clientId) throws Exception {
        String body = form(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", clientId
        ));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200)
            throw new RuntimeException("refresh failed: " + res.statusCode() + " " + res.body());

        return gson.fromJson(res.body(), JsonObject.class);
    }

    private static HttpServer startCallbackServer(CompletableFuture<String> codeFuture) throws Exception {
        URI uri = URI.create(REDIRECT_URI);
        int port = uri.getPort();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/callback", exchange -> {
            Map<String, String> params = splitQuery(exchange.getRequestURI().getRawQuery());
            String code = params.get("code");
            String state = params.get("state");

            String response;
            int status;

            if (code == null || state == null || !state.equals(lastState)) {
                status = 400;
                response = "Spotify auth failed. You can close this tab.";
            } else {
                status = 200;
                response = "Spotify connected ✅ You can close this tab.";
                codeFuture.complete(code);
            }

            exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });

        server.start();
        return server;
    }

    private static Map<String, String> splitQuery(String query) {
        if (query == null || query.isBlank()) return Map.of();
        return java.util.Arrays.stream(query.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(
                        a -> dec(a[0]),
                        a -> a.length > 1 ? dec(a[1]) : ""
                ));
    }

    private static String form(Map<String, String> data) {
        return data.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String dec(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }

    private static String randomBase64Url(int lenBytes) {
        byte[] b = new byte[lenBytes];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String sha256Base64Url(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}