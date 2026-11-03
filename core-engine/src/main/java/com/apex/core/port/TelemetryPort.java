package com.apex.core.port;

import java.util.Map;

public interface TelemetryPort {
    void recordEvent(String eventName, Map<String, String> attributes);
    void recordLatency(String operation, long durationMillis);
}
