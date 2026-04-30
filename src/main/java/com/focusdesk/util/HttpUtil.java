package com.focusdesk.util;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

public final class HttpUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private HttpUtil() {}

    public static String get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return response.body();
    }

    public static String postForm(String url, Map<String, String> form, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                .header("Content-Type", "application/x-www-form-urlencoded");
        headers.forEach(builder::header);
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return response.body();
    }

    private static String encodeForm(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        form.forEach((key, value) -> joiner.add(url(key) + "=" + url(value)));
        return joiner.toString();
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void ensureSuccess(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
    }
}
