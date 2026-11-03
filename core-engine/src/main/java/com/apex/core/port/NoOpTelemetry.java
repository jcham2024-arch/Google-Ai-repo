package com.apex.core.port;

import java.util.Collections;
import java.util.Map;

/**
 * Standalone zero-dependency fallback implementation of {@link TelemetryPort} that silently drops all signals.
 */
public final class NoOpTelemetry implements TelemetryPort {
    public static final NoOpTelemetry INSTANCE = new NoOpTelemetry();

    public static final Span NO_OP_SPAN = new Span() {
        @Override
        public void setAttribute(String key, String value) {}

        @Override
        public void recordException(Throwable throwable) {}

        @Override
        public void close() {}
    };

    @Override
    public Span startSpan(String name, Map<String, String> attributes) {
        return NO_OP_SPAN;
    }

    @Override
    public void counter(String name, long delta, Map<String, String> tags) {}

    @Override
    public void histogram(String name, long valueMs, Map<String, String> tags) {}

    @Override
    public void log(Level level, String event, Map<String, Object> fields) {}
}
