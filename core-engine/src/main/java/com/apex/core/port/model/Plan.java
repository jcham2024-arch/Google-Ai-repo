package com.apex.core.port.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Plan {
    private final String goal;
    private final List<PlanStep> steps;

    public Plan(String goal, List<PlanStep> steps) {
        this.goal = goal != null ? goal : "";
        this.steps = steps != null ? Collections.unmodifiableList(steps) : Collections.emptyList();
    }

    public String getGoal() { return goal; }
    public List<PlanStep> getSteps() { return steps; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plan plan = (Plan) o;
        return Objects.equals(goal, plan.goal) &&
               Objects.equals(steps, plan.steps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goal, steps);
    }

    @Override
    public String toString() {
        return "Plan{goal='" + goal + "', steps=" + steps.size() + "}";
    }
}
