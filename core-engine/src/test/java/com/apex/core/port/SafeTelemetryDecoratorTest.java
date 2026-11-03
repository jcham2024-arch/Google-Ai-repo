package com.apex.core.port;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test verifying fault isolation (Constraint C9) and sensitive field redaction in {@link SafeTelemetryDecorator}.
 */
public class SafeTelemetryDecoratorTest {

    @Test
    public void testThrowingDelegateDoesNotCrashCaller() {
        TelemetryPort throwingDelegate = new TelemetryPort() {
            @Override
            public Span startSpan(String name, Map<String, String> attributes) {
                throw new RuntimeException("Telemetry backend crashed!");
            }

            @Override
            public void counter(String name, long delta, Map<String, String> tags) {
                throw new RuntimeException("Telemetry backend crashed!");
            }

            @Override
            public void histogram(String name, long valueMs, Map<String, String> tags) {
                throw new RuntimeException("Telemetry backend crashed!");
            }

            @Override
            public void log(Level level, String event, Map<String, Object> fields) {
                throw new RuntimeException("Telemetry backend crashed!");
            }
        };

        SafeTelemetryDecorator decorator = new SafeTelemetryDecorator(throwingDelegate);

        // All calls must succeed silently without throwing
        try (TelemetryPort.Span span = decorator.startSpan("test.span", Collections.emptyMap())) {
            assertNotNull(span);
            span.setAttribute("key", "val");
            span.recordException(new Exception("test"));
        }

        decorator.counter("test.counter", 1, Collections.emptyMap());
        decorator.histogram("test.histogram", 100, Collections.emptyMap());
        decorator.log(TelemetryPort.Level.INFO, "test.event", Collections.emptyMap());
    }

    @Test
    public void testSecretRedaction() {
        AtomicBoolean logged = new AtomicBoolean(false);
        Map<String, Object> captured = new HashMap<>();

        TelemetryPort capturingDelegate = new TelemetryPort() {
            @Override
            public Span startSpan(String name, Map<String, String> attributes) {
                return NoOpTelemetry.NO_OP_SPAN;
            }

            @Override
            public void counter(String name, long delta, Map<String, String> tags) {}

            @Override
            public void histogram(String name, long valueMs, Map<String, String> tags) {}

            @Override
            public void log(Level level, String event, Map<String, Object> fields) {
                logged.set(true);
                captured.putAll(fields);
            }
        };

        SafeTelemetryDecorator decorator = new SafeTelemetryDecorator(capturingDelegate);

        Map<String, Object> fields = new HashMap<>();
        fields.put("user_id", "user_123");
        fields.put("api_key", "sk-secret-12345");
        fields.put("auth_token", "bearer-abc");
        fields.put("user_password", "pass123");

        decorator.log(TelemetryPort.Level.INFO, "login_event", fields);

        assertTrue(logged.get());
        assertEquals("user_123", captured.get("user_id"));
        assertEquals("[REDACTED]", captured.get("api_key"));
        assertEquals("[REDACTED]", captured.get("auth_token"));
        assertEquals("[REDACTED]", captured.get("user_password"));
    }
}
