package com.apex.security.interlock;

import com.apex.core.port.SafeTelemetryDecorator;
import com.apex.core.port.TelemetryPort;
import com.apex.core.port.ToolExecutorPort;
import com.apex.core.port.model.ExecutionResult;
import com.apex.core.port.model.ToolCall;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor wrapping {@link ToolExecutorPort} that automatically records spans, metrics, and structured logs per tool call.
 */
public class ToolInvocationInterceptor implements ToolExecutorPort {
    private final ToolExecutorPort delegate;
    private final TelemetryPort telemetry;

    public ToolInvocationInterceptor(ToolExecutorPort delegate, TelemetryPort telemetry) {
        this.delegate = delegate;
        this.telemetry = new SafeTelemetryDecorator(telemetry);
    }

    @Override
    public ExecutionResult execute(ToolCall toolCall) {
        String toolName = toolCall != null ? toolCall.getToolName() : "unknown_tool";
        Map<String, String> spanAttrs = new HashMap<>();
        spanAttrs.put("tool.id", toolName);

        long start = System.currentTimeMillis();
        try (TelemetryPort.Span span = telemetry.startSpan("tool.invoke", spanAttrs)) {
            ExecutionResult result = null;
            try {
                result = delegate != null ? delegate.execute(toolCall) : ExecutionResult.fail(toolCall != null ? toolCall.getId() : "", "No delegate executor");
            } catch (Throwable t) {
                span.recordException(t);
                telemetry.counter("security.denials", 1, spanAttrs);
                throw t;
            }

            long duration = System.currentTimeMillis() - start;
            boolean isOk = result != null && result.isSuccess();
            String outcome = isOk ? "EXECUTED_OK" : "DENIED_OR_ERROR";

            span.setAttribute("outcome", outcome);

            Map<String, Object> logFields = new HashMap<>();
            logFields.put("tool.id", toolName);
            logFields.put("duration.ms", duration);
            logFields.put("outcome", outcome);

            telemetry.log(TelemetryPort.Level.INFO, "tool.invoked", logFields);
            telemetry.histogram("tool.duration_ms", duration, spanAttrs);
            telemetry.counter("tool.invocations", 1, spanAttrs);

            if (!isOk) {
                telemetry.counter("security.denials", 1, spanAttrs);
            }

            return result;
        }
    }
}
