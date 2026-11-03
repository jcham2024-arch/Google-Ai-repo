package com.apex.core.port.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ApprovalRequest {
    private final String requestId;
    private final String toolName;
    private final Map<String, String> arguments;
    private final String riskLevel;
    private final String reason;

    public ApprovalRequest(String requestId, String toolName, Map<String, String> arguments, String riskLevel, String reason) {
        this.requestId = requestId != null ? requestId : "";
        this.toolName = toolName != null ? toolName : "";
        this.arguments = arguments != null ? Collections.unmodifiableMap(arguments) : Collections.emptyMap();
        this.riskLevel = riskLevel != null ? riskLevel : "L3";
        this.reason = reason != null ? reason : "";
    }

    public String getRequestId() { return requestId; }
    public String getToolName() { return toolName; }
    public Map<String, String> getArguments() { return arguments; }
    public String getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalRequest that = (ApprovalRequest) o;
        return Objects.equals(requestId, that.requestId) &&
               Objects.equals(toolName, that.toolName) &&
               Objects.equals(arguments, that.arguments) &&
               Objects.equals(riskLevel, that.riskLevel) &&
               Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, toolName, arguments, riskLevel, reason);
    }

    @Override
    public String toString() {
        return "ApprovalRequest{id='" + requestId + "', tool='" + toolName + "', risk='" + riskLevel + "'}";
    }
}
