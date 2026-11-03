package com.apex.core.port.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value representation of an outbound HTTP request.
 */
public final class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final long timeoutMs;

    public HttpRequest(String method, String url, Map<String, String> headers, String body, long timeoutMs) {
        this.method = method != null ? method : "GET";
        this.url = url != null ? url : "";
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.body = body;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 30_000L;
    }

    public static HttpRequest post(String url, Map<String, String> headers, String body) {
        return new HttpRequest("POST", url, headers, body, 30_000L);
    }

    public static HttpRequest get(String url, Map<String, String> headers) {
        return new HttpRequest("GET", url, headers, null, 30_000L);
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public long getTimeoutMs() { return timeoutMs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequest that = (HttpRequest) o;
        return timeoutMs == that.timeoutMs &&
               Objects.equals(method, that.method) &&
               Objects.equals(url, that.url) &&
               Objects.equals(headers, that.headers) &&
               Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, url, headers, body, timeoutMs);
    }

    @Override
    public String toString() {
        return "HttpRequest{method='" + method + "', url='" + url + "'}";
    }
}
