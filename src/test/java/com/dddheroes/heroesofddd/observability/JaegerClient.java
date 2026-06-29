package com.dddheroes.heroesofddd.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Minimal client for the Jaeger Query HTTP API (port 16686), used by the tracing integration test
 * to read back the spans an Axon application exported via OTLP.
 * <p>
 * Jaeger v2 keeps the v1 query API: {@code GET /api/traces?service=<name>&lookback=<dur>&limit=<n>}
 * returns {@code {"data":[{"spans":[{"operationName":..,"tags":[{"key":..,"value":..}]}]}]}}.
 * We flatten every span across every returned trace into a flat {@link Span} list so the test can
 * assert on operation names and message attributes regardless of how Jaeger groups them into traces.
 */
final class JaegerClient {

    /** A single span flattened out of the Jaeger response, with its tags as a flat map. */
    record Span(String operationName, Map<String, String> tags) {

        boolean operationMatches(Pattern pattern) {
            return pattern.matcher(operationName).matches();
        }

        boolean hasTag(String key, String value) {
            return value.equals(tags.get(key));
        }
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(5))
                                                    .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    JaegerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** All spans exported by {@code serviceName} within the lookback window, flattened. */
    List<Span> fetchSpans(String serviceName, Duration lookback) {
        var url = "%s/api/traces?service=%s&lookback=%ss&limit=2000".formatted(
                baseUrl,
                URLEncoder.encode(serviceName, StandardCharsets.UTF_8),
                lookback.toSeconds()
        );
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                                     .timeout(Duration.ofSeconds(10))
                                     .GET()
                                     .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // Service not registered yet (no spans exported) → empty, let the caller retry.
                return List.of();
            }
            return parseSpans(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to query Jaeger at " + url, e);
        }
    }

    private List<Span> parseSpans(String body) throws Exception {
        var spans = new ArrayList<Span>();
        var data = objectMapper.readTree(body).path("data");
        if (!data.isArray()) {
            return spans;
        }
        for (JsonNode trace : data) {
            for (JsonNode span : trace.path("spans")) {
                var tags = new HashMap<String, String>();
                for (JsonNode tag : span.path("tags")) {
                    tags.put(tag.path("key").asText(), tag.path("value").asText());
                }
                spans.add(new Span(span.path("operationName").asText(), tags));
            }
        }
        return spans;
    }
}
