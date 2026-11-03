package com.apex.core.port.model;

import java.util.Objects;

public final class ExecutionResult {
    private final String toolCallId;
    private final String output;
    private final boolean success;
    private final String errorMessage;

    public ExecutionResult(String toolCallId, String output, boolean success, String errorMessage) {
        this.toolCallId = toolCallId != null ? toolCallId : "";
        this.output = output != null ? output : "";
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ExecutionResult ok(String toolCallId, String output) {
        return new ExecutionResult(toolCallId, output, true, null);
    }

    public static ExecutionResult fail(String toolCallId, String errorMessage) {
        return new ExecutionResult(toolCallId, "", false, errorMessage);
    }

    public String getToolCallId() { return toolCallId; }
    public String getOutput() { return output; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResult that = (ExecutionResult) o;
        return success == that.success &&
               Objects.equals(toolCallId, that.toolCallId) &&
               Objects.equals(output, that.output) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolCallId, output, success, errorMessage);
    }

    @Override
    public String toString() {
        return "ExecutionResult{success=" + success + ", output='" + output + "'}";
    }
}
