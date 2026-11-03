package com.apex.core.port;

import java.util.Map;

/**
 * Universal telemetry port abstraction providing structured logging, metric counters/histograms, and span tracing.
 */
public interface TelemetryPort {

    enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    interface Span extends AutoCloseable {
        void setAttribute(String key, String value);
        void recordException(Throwable throwable);
        @Override
        void close();
    }

    Span startSpan(String name, Map<String, String> attributes);
    void counter(String name, long delta, Map<String, String> tags);
    void histogram(String name, long valueMs, Map<String, String> tags);
    void log(Level level, String event, Map<String, Object> fields);

    // Backward-compatible utility methods
    default void recordEvent(String eventName, Map<String, String> attributes) {
        log(Level.INFO, eventName, attributes != null ? (Map) attributes : java.util.Collections.emptyMap());
    }

    default void recordLatency(String operation, long durationMillis) {
        histogram(operation, durationMillis, java.util.Collections.emptyMap());
    }
}
