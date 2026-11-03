package com.apex.core.port.model;

import java.util.Objects;

/**
 * Streaming chunk emitted during Server-Sent Events (SSE) or streaming requests.
 */
public final class HttpChunk {
    private final String text;
    private final boolean done;
    private final boolean error;
    private final boolean retryable;
    private final String cause;

    public HttpChunk(String text, boolean done, boolean error, boolean retryable, String cause) {
        this.text = text != null ? text : "";
        this.done = done;
        this.error = error;
        this.retryable = retryable;
        this.cause = cause;
    }

    public static HttpChunk data(String text) {
        return new HttpChunk(text, false, false, false, null);
    }

    public static HttpChunk done() {
        return new HttpChunk("", true, false, false, null);
    }

    public static HttpChunk error(boolean retryable, String cause) {
        return new HttpChunk("", false, true, retryable, cause);
    }

    public String getText() { return text; }
    public boolean isDone() { return done; }
    public boolean isError() { return error; }
    public boolean isRetryable() { return retryable; }
    public String getCause() { return cause; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpChunk httpChunk = (HttpChunk) o;
        return done == httpChunk.done &&
               error == httpChunk.error &&
               retryable == httpChunk.retryable &&
               Objects.equals(text, httpChunk.text) &&
               Objects.equals(cause, httpChunk.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, done, error, retryable, cause);
    }

    @Override
    public String toString() {
        if (done) return "HttpChunk[DONE]";
        if (error) return "HttpChunk[ERROR: " + cause + "]";
        return "HttpChunk[DATA: " + text + "]";
    }
}
