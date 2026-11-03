package com.apex.core.port.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable declaration of required parameters and schema constraints for a tool worker.
 */
public final class ToolSchema {
    private final String toolName;
    private final Set<String> requiredParameters;

    public ToolSchema(String toolName, Set<String> requiredParameters) {
        this.toolName = toolName != null ? toolName : "";
        this.requiredParameters = requiredParameters != null ? Collections.unmodifiableSet(requiredParameters) : Collections.emptySet();
    }

    public static ToolSchema create(String toolName, String... requiredParams) {
        Set<String> set = new HashSet<>();
        if (requiredParams != null) {
            Collections.addAll(set, requiredParams);
        }
        return new ToolSchema(toolName, set);
    }

    public String getToolName() { return toolName; }
    public Set<String> getRequiredParameters() { return requiredParameters; }

    public boolean validate(Map<String, String> arguments) {
        if (requiredParameters.isEmpty()) return true;
        if (arguments == null) return false;
        for (String required : requiredParameters) {
            if (!arguments.containsKey(required) || arguments.get(required) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolSchema that = (ToolSchema) o;
        return Objects.equals(toolName, that.toolName) &&
               Objects.equals(requiredParameters, that.requiredParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolName, requiredParameters);
    }
}
