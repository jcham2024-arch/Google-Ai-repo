package com.apex.core.port.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PlanStep {
    private final int index;
    private final String description;
    private final String requiredTool;
    private final boolean completed;

    public PlanStep(int index, String description, String requiredTool, boolean completed) {
        this.index = index;
        this.description = description != null ? description : "";
        this.requiredTool = requiredTool != null ? requiredTool : "";
        this.completed = completed;
    }

    public int getIndex() { return index; }
    public String getDescription() { return description; }
    public String getRequiredTool() { return requiredTool; }
    public boolean isCompleted() { return completed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanStep planStep = (PlanStep) o;
        return index == planStep.index &&
               completed == planStep.completed &&
               Objects.equals(description, planStep.description) &&
               Objects.equals(requiredTool, planStep.requiredTool);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, description, requiredTool, completed);
    }

    @Override
    public String toString() {
        return "PlanStep{" + index + ": " + description + "}";
    }
}
