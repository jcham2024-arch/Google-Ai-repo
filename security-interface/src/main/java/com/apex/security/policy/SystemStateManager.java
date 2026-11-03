package com.apex.security.policy;

import com.apex.core.port.SafeTelemetryDecorator;
import com.apex.core.port.TelemetryPort;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages operational state transitions (NORMAL, DEGRADED, SAFE_MODE) and records state metric gauges.
 */
public class SystemStateManager {

    public enum State {
        NORMAL(0),
        DEGRADED(1),
        SAFE_MODE(2);

        private final int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final TelemetryPort telemetry;
    private volatile State currentState = State.NORMAL;

    public SystemStateManager(TelemetryPort telemetry) {
        this.telemetry = new SafeTelemetryDecorator(telemetry);
        emitStateMetric(currentState);
    }

    public State getCurrentState() {
        return currentState;
    }

    public synchronized void transitionTo(State newState, String reason) {
        if (newState != null && newState != currentState) {
            State oldState = currentState;
            currentState = newState;

            Map<String, Object> logFields = new HashMap<>();
            logFields.put("old_state", oldState.name());
            logFields.put("new_state", newState.name());
            logFields.put("reason", reason != null ? reason : "unspecified");

            telemetry.log(TelemetryPort.Level.WARN, "system.state_changed", logFields);
            emitStateMetric(newState);
        }
    }

    private void emitStateMetric(State state) {
        telemetry.histogram("system.state", state.getValue(), Collections.emptyMap());
    }
}
