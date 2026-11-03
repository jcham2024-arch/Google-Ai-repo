package com.apex.core.port.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of an HTTP response or transport failure.
 */
public final class HttpResponse {
    private final boolean success;
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;
    private final boolean retryable;
    private final String cause;

    public HttpResponse(boolean success, int statusCode, Map<String, String> headers, String body, boolean retryable, String cause) {
        this.success = success;
        this.statusCode = statusCode;
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.body = body != null ? body : "";
        this.retryable = retryable;
        this.cause = cause;
    }

    public static HttpResponse success(int status, Map<String, String> headers, String body) {
        return new HttpResponse(true, status, headers, body, false, null);
    }

    public static HttpResponse failure(Integer statusCode, boolean retryable, String cause) {
        return new HttpResponse(false, statusCode != null ? statusCode : 0, Collections.emptyMap(), "", retryable, cause);
    }

    public boolean isSuccess() { return success; }
    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public boolean isRetryable() { return retryable; }
    public String getCause() { return cause; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpResponse that = (HttpResponse) o;
        return success == that.success &&
               statusCode == that.statusCode &&
               retryable == that.retryable &&
               Objects.equals(headers, that.headers) &&
               Objects.equals(body, that.body) &&
               Objects.equals(cause, that.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, statusCode, headers, body, retryable, cause);
    }

    @Override
    public String toString() {
        return "HttpResponse{success=" + success + ", status=" + statusCode + "}";
    }
}
