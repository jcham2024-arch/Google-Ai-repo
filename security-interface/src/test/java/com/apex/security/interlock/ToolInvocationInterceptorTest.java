package com.apex.security.interlock;

import com.apex.core.port.TelemetryPort;
import com.apex.core.port.ToolExecutorPort;
import com.apex.core.port.model.ExecutionResult;
import com.apex.core.port.model.ToolCall;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test verifying telemetry signal emissions by {@link ToolInvocationInterceptor}.
 */
public class ToolInvocationInterceptorTest {

    @Test
    public void testToolInvocationEmitsMetricsAndSpans() {
        AtomicBoolean spanStarted = new AtomicBoolean(false);
        AtomicBoolean counterCalled = new AtomicBoolean(false);
        AtomicBoolean histogramCalled = new AtomicBoolean(false);
        AtomicBoolean loggedCalled = new AtomicBoolean(false);

        TelemetryPort testTelemetry = new TelemetryPort() {
            @Override
            public Span startSpan(String name, Map<String, String> attributes) {
                spanStarted.set(true);
                return new Span() {
                    @Override
                    public void setAttribute(String key, String value) {}

                    @Override
                    public void recordException(Throwable throwable) {}

                    @Override
                    public void close() {}
                };
            }

            @Override
            public void counter(String name, long delta, Map<String, String> tags) {
                counterCalled.set(true);
            }

            @Override
            public void histogram(String name, long valueMs, Map<String, String> tags) {
                histogramCalled.set(true);
            }

            @Override
            public void log(Level level, String event, Map<String, Object> fields) {
                loggedCalled.set(true);
            }
        };

        ToolExecutorPort delegate = toolCall -> ExecutionResult.ok(toolCall.getId(), "OK");

        ToolInvocationInterceptor interceptor = new ToolInvocationInterceptor(delegate, testTelemetry);
        ToolCall call = new ToolCall("tc1", "read_file", Collections.emptyMap());

        ExecutionResult res = interceptor.execute(call);

        assertTrue(res.isSuccess());
        assertTrue(spanStarted.get());
        assertTrue(counterCalled.get());
        assertTrue(histogramCalled.get());
        assertTrue(loggedCalled.get());
    }
}
