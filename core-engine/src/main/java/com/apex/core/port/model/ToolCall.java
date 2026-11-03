package com.apex.core.port.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ToolCall {
    private final String id;
    private final String toolName;
    private final Map<String, String> arguments;

    public ToolCall(String id, String toolName, Map<String, String> arguments) {
        this.id = id != null ? id : "";
        this.toolName = toolName != null ? toolName : "";
        this.arguments = arguments != null ? Collections.unmodifiableMap(arguments) : Collections.emptyMap();
    }

    public String getId() { return id; }
    public String getToolName() { return toolName; }
    public Map<String, String> getArguments() { return arguments; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolCall toolCall = (ToolCall) o;
        return Objects.equals(id, toolCall.id) &&
               Objects.equals(toolName, toolCall.toolName) &&
               Objects.equals(arguments, toolCall.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, toolName, arguments);
    }

    @Override
    public String toString() {
        return "ToolCall{id='" + id + "', toolName='" + toolName + "', args=" + arguments + "}";
    }
}
