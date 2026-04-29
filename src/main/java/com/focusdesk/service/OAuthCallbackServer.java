package com.focusdesk.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class OAuthCallbackServer {

    private HttpServer server;

    public CompletableFuture<Map<String, String>> waitForCallback(String path, int port) throws IOException {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext(path, exchange -> handle(exchange, future));
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "oauth-callback-server");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        return future;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handle(HttpExchange exchange, CompletableFuture<Map<String, String>> future) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String html = """
                <html><body style="font-family:sans-serif;padding:24px;">
                <h2>FocusDesk</h2>
                <p>Google Calendar connection complete. You can close this tab.</p>
                </body></html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        future.complete(query);
        stop();
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            values.put(key, value);
        }
        return values;
    }
}
