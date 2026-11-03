package com.apex.security.interlock;

import com.apex.core.port.ApprovalPort;
import com.apex.core.port.ToolExecutorPort;
import com.apex.core.port.model.ApprovalDecision;
import com.apex.core.port.model.ApprovalRequest;
import com.apex.core.port.model.ExecutionResult;
import com.apex.core.port.model.ToolCall;
import com.apex.core.tool.ToolRegistry;
import com.apex.core.tool.ToolWorker;
import com.apex.security.audit.AuditLogManager;
import com.apex.security.policy.BudgetExhaustedException;
import com.apex.security.policy.RiskLevel;
import com.apex.security.policy.ToolRiskRegistry;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fail-closed security interlock boundary enforcing risk gating, human authorization, and iteration budgets.
 */
public class SecurityInterlock implements ToolExecutorPort {
    public static final int DEFAULT_MAX_ITERATIONS = 10;

    private final ToolRegistry toolRegistry;
    private final ToolRiskRegistry riskRegistry;
    private final AuditLogManager auditLogManager;
    private final ApprovalPort approvalPort;
    private final int maxIterations;
    private final AtomicInteger currentIteration = new AtomicInteger(0);

    public SecurityInterlock(ToolRegistry toolRegistry, ToolRiskRegistry riskRegistry,
                              AuditLogManager auditLogManager, ApprovalPort approvalPort) {
        this(toolRegistry, riskRegistry, auditLogManager, approvalPort, DEFAULT_MAX_ITERATIONS);
    }

    public SecurityInterlock(ToolRegistry toolRegistry, ToolRiskRegistry riskRegistry,
                              AuditLogManager auditLogManager, ApprovalPort approvalPort,
                              int maxIterations) {
        this.toolRegistry = toolRegistry;
        this.riskRegistry = riskRegistry != null ? riskRegistry : new ToolRiskRegistry();
        this.auditLogManager = auditLogManager != null ? auditLogManager : new AuditLogManager();
        this.approvalPort = approvalPort;
        this.maxIterations = maxIterations > 0 ? maxIterations : DEFAULT_MAX_ITERATIONS;
    }

    public void resetIterationCount() {
        currentIteration.set(0);
    }

    @Override
    public ExecutionResult execute(ToolCall toolCall) {
        if (currentIteration.incrementAndGet() > maxIterations) {
            long now = System.currentTimeMillis();
            auditLogManager.createAndAppend(now, toolCall != null ? toolCall.getToolName() : "unknown", "L4", "DENIED_BUDGET_EXHAUSTED", "Max iterations exceeded");
            throw new BudgetExhaustedException("Tool execution limit exceeded max iterations: " + maxIterations);
        }

        if (toolCall == null || toolCall.getToolName() == null || toolCall.getToolName().trim().isEmpty()) {
            long now = System.currentTimeMillis();
            auditLogManager.createAndAppend(now, "invalid_call", "L3", "DENIED_MALFORMED", "Tool call or tool name was null/empty");
            return ExecutionResult.fail("", "Security denial: Malformed tool call.");
        }

        String toolName = toolCall.getToolName().trim();
        RiskLevel risk = riskRegistry.getRiskLevel(toolName);
        long now = System.currentTimeMillis();

        if (risk.requiresApproval()) {
            if (approvalPort == null) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_NO_APPROVER", toolCall.getArguments().toString());
                return ExecutionResult.fail(toolCall.getId(), "Execution denied: Tool requires approval but no ApprovalPort configured.");
            }

            ApprovalRequest request = new ApprovalRequest(
                toolCall.getId(),
                toolName,
                toolCall.getArguments(),
                risk.name(),
                "Tool execution requires user authorization."
            );

            ApprovalDecision decision;
            try {
                decision = approvalPort.requestApproval(request);
            } catch (Throwable t) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_APPROVER_EXCEPTION", toolCall.getArguments().toString());
                return ExecutionResult.fail(toolCall.getId(), "Execution denied: ApprovalPort threw exception: " + t.getMessage());
            }

            if (decision == null || !decision.isApproved()) {
                String comment = decision != null ? decision.getApproverComment() : "Null decision returned";
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED", toolCall.getArguments().toString());
                return ExecutionResult.fail(toolCall.getId(), "Tool execution denied by user: " + comment);
            }

            if (!request.getRequestId().equals(decision.getRequestId())) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_REQUEST_ID_MISMATCH", toolCall.getArguments().toString());
                return ExecutionResult.fail(toolCall.getId(), "Execution denied: Approval decision request ID mismatch.");
            }
        }

        // Execute tool worker
        Optional<ToolWorker> workerOpt = toolRegistry != null ? toolRegistry.getWorker(toolName) : Optional.empty();
        if (!workerOpt.isPresent()) {
            auditLogManager.createAndAppend(now, toolName, risk.name(), "ERROR_NOT_FOUND", toolCall.getArguments().toString());
            return ExecutionResult.fail(toolCall.getId(), "Tool not found: " + toolName);
        }

        try {
            String output = workerOpt.get().execute(toolCall.getArguments());
            auditLogManager.createAndAppend(now, toolName, risk.name(), "EXECUTED_OK", toolCall.getArguments().toString());
            return ExecutionResult.ok(toolCall.getId(), output);
        } catch (Exception e) {
            auditLogManager.createAndAppend(now, toolName, risk.name(), "ERROR_THROWN", toolCall.getArguments().toString());
            return ExecutionResult.fail(toolCall.getId(), "Tool error: " + e.getMessage());
        }
    }

    public AuditLogManager getAuditLogManager() {
        return auditLogManager;
    }

    public ToolRiskRegistry getRiskRegistry() {
        return riskRegistry;
    }
}
