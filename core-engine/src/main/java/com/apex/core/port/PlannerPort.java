package com.apex.core.port;

import com.apex.core.port.model.Plan;
import com.apex.core.port.model.PlanStep;

public interface PlannerPort {
    Plan createPlan(String goal, String context);
    Plan rePlan(Plan currentPlan, PlanStep failedStep, String failureReason);
}
