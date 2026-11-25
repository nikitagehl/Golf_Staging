package com.nikitagehlod.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks links over HTTP without opening them in the browser (fast broken-link check).
 * A link counts as broken when it answers with status 400 or higher, or does not answer at all.
 */
public final class LinkChecker {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private LinkChecker() {
    }

    /** Returns the HTTP status for the URL, or -1 when the request failed. */
    public static int status(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (Selenium sanity link check)")
                    .GET()
                    .build();
            return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Returns only the broken links (url -> status). An empty map means every link is fine. */
    public static Map<String, Integer> findBroken(Collection<String> urls) {
        Map<String, Integer> broken = new LinkedHashMap<>();
        for (String url : urls) {
            int code = status(url);
            if (code < 200 || code >= 400) {
                broken.put(url, code);
            }
        }
        return broken;
    }
}
