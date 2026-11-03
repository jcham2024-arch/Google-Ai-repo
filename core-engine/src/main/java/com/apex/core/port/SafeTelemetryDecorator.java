package com.apex.core.port;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fault-isolating decorator wrapping any {@link TelemetryPort} delegate.
 * <p>
 * Security & Reliability Invariants:
 * <ul>
 *   <li>Catches all {@link Throwable} exceptions from the underlying delegate and swallows them safely (Constraint C9).</li>
 *   <li>Redacts sensitive fields matching key names like 'token', 'secret', 'key', or 'password'.</li>
 * </ul>
 */
public final class SafeTelemetryDecorator implements TelemetryPort {

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i).*(token|secret|key|password).*");
    private final TelemetryPort delegate;

    public SafeTelemetryDecorator(TelemetryPort delegate) {
        this.delegate = delegate != null ? delegate : NoOpTelemetry.INSTANCE;
    }

    @Override
    public Span startSpan(String name, Map<String, String> attributes) {
        try {
            Span span = delegate.startSpan(name, sanitizeStringMap(attributes));
            if (span == null) {
                return NoOpTelemetry.NO_OP_SPAN;
            }
            return new SafeSpan(span);
        } catch (Throwable t) {
            return NoOpTelemetry.NO_OP_SPAN;
        }
    }

    @Override
    public void counter(String name, long delta, Map<String, String> tags) {
        try {
            delegate.counter(name, delta, sanitizeStringMap(tags));
        } catch (Throwable ignored) {}
    }

    @Override
    public void histogram(String name, long valueMs, Map<String, String> tags) {
        try {
            delegate.histogram(name, valueMs, sanitizeStringMap(tags));
        } catch (Throwable ignored) {}
    }

    @Override
    public void log(Level level, String event, Map<String, Object> fields) {
        try {
            delegate.log(level != null ? level : Level.INFO, event, sanitizeObjectMap(fields));
        } catch (Throwable ignored) {}
    }

    private static Map<String, String> sanitizeStringMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return map;
        Map<String, String> cleaned = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() != null && SENSITIVE_KEY_PATTERN.matcher(entry.getKey()).matches()) {
                cleaned.put(entry.getKey(), "[REDACTED]");
            } else {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return cleaned;
    }

    private static Map<String, Object> sanitizeObjectMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return map;
        Map<String, Object> cleaned = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && SENSITIVE_KEY_PATTERN.matcher(entry.getKey()).matches()) {
                cleaned.put(entry.getKey(), "[REDACTED]");
            } else {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }
        return cleaned;
    }

    private static class SafeSpan implements Span {
        private final Span delegateSpan;

        SafeSpan(Span delegateSpan) {
            this.delegateSpan = delegateSpan;
        }

        @Override
        public void setAttribute(String key, String value) {
            try {
                if (key != null && SENSITIVE_KEY_PATTERN.matcher(key).matches()) {
                    delegateSpan.setAttribute(key, "[REDACTED]");
                } else {
                    delegateSpan.setAttribute(key, value);
                }
            } catch (Throwable ignored) {}
        }

        @Override
        public void recordException(Throwable throwable) {
            try {
                delegateSpan.recordException(throwable);
            } catch (Throwable ignored) {}
        }

        @Override
        public void close() {
            try {
                delegateSpan.close();
            } catch (Throwable ignored) {}
        }
    }
}
