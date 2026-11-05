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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final ConcurrentHashMap<String, AtomicInteger> iterationCounters = new ConcurrentHashMap<>();
    private final AtomicBoolean toolsPaused = new AtomicBoolean(false);

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

    /**
     * Gets or creates an iteration counter for the given conversation/session ID.
     */
    private AtomicInteger getIterationCounter(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "default";
        }
        return iterationCounters.computeIfAbsent(conversationId, k -> new AtomicInteger(0));
    }

    public void resetIterationCount(String conversationId) {
        AtomicInteger counter = iterationCounters.get(conversationId);
        if (counter != null) {
            counter.set(0);
        }
    }

    public void resetAllIterationCounts() {
        iterationCounters.clear();
    }

    /**
     * Pauses or resumes tool execution globally.
     * When paused, all tool executions will be denied.
     */
    /**
     * Builds the audited argument map for a pre-execution denial: the original tool arguments plus
     * the denial reason, so the reason is inside the hashed audit domain instead of being dropped.
     */
    private static Map<String, String> denialArguments(ToolCall toolCall, String reason) {
        final Map<String, String> arguments = new LinkedHashMap<>();
        if (toolCall != null && toolCall.getArguments() != null) {
            arguments.putAll(toolCall.getArguments());
        }
        arguments.put("_reason", reason);
        return arguments;
    }

    public void setToolsPaused(boolean paused) {
        toolsPaused.set(paused);
    }

    /**
     * Returns whether tool execution is currently paused.
     */
    public boolean isToolsPaused() {
        return toolsPaused.get();
    }

    @Override
    public ExecutionResult execute(ToolCall toolCall) {
        // Check if tools are paused (kill switch)
        if (toolsPaused.get()) {
            long now = System.currentTimeMillis();
            auditLogManager.createAndAppend(now, toolCall != null ? toolCall.getToolName() : "unknown", "L3", "DENIED_TOOLS_PAUSED",
                denialArguments(toolCall, "Tool execution paused by kill switch"));
            throw new BudgetExhaustedException("Tool execution paused by kill switch");
        }

        String conversationId = toolCall != null && toolCall.getArguments() != null 
            ? toolCall.getArguments().get("_conversation_id") 
            : "default";
        
        AtomicInteger counter = getIterationCounter(conversationId);
        
        if (counter.incrementAndGet() > maxIterations) {
            long now = System.currentTimeMillis();
            auditLogManager.createAndAppend(now, toolCall != null ? toolCall.getToolName() : "unknown", "L4", "DENIED_BUDGET_EXHAUSTED",
                denialArguments(toolCall, "Max iterations exceeded for conversation: " + conversationId));
            throw new BudgetExhaustedException("Tool execution limit exceeded max iterations: " + maxIterations + " for conversation: " + conversationId);
        }

        if (toolCall == null || toolCall.getToolName() == null || toolCall.getToolName().trim().isEmpty()) {
            long now = System.currentTimeMillis();
            auditLogManager.createAndAppend(now, "invalid_call", "L3", "DENIED_MALFORMED",
                denialArguments(toolCall, "Tool call or tool name was null/empty"));
            return ExecutionResult.fail("", "Security denial: Malformed tool call.");
        }

        String toolName = toolCall.getToolName().trim();
        RiskLevel risk = riskRegistry.getRiskLevel(toolName);
        long now = System.currentTimeMillis();

        if (risk.requiresApproval()) {
            if (approvalPort == null) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_NO_APPROVER", toolCall.getArguments());
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
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_APPROVER_EXCEPTION", toolCall.getArguments());
                return ExecutionResult.fail(toolCall.getId(), "Execution denied: ApprovalPort threw exception: " + t.getMessage());
            }

            if (decision == null || !decision.isApproved()) {
                String comment = decision != null ? decision.getApproverComment() : "Null decision returned";
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED", toolCall.getArguments());
                return ExecutionResult.fail(toolCall.getId(), "Tool execution denied by user: " + comment);
            }

            if (!request.getRequestId().equals(decision.getRequestId())) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED_REQUEST_ID_MISMATCH", toolCall.getArguments());
                return ExecutionResult.fail(toolCall.getId(), "Execution denied: Approval decision request ID mismatch.");
            }
        }

        // Execute tool worker
        Optional<ToolWorker> workerOpt = toolRegistry != null ? toolRegistry.getWorker(toolName) : Optional.empty();
        if (!workerOpt.isPresent()) {
            auditLogManager.createAndAppend(now, toolName, risk.name(), "ERROR_NOT_FOUND", toolCall.getArguments());
            return ExecutionResult.fail(toolCall.getId(), "Tool not found: " + toolName);
        }

        try {
            String output = workerOpt.get().execute(toolCall.getArguments());
            auditLogManager.createAndAppend(now, toolName, risk.name(), "EXECUTED_OK", toolCall.getArguments());
            return ExecutionResult.ok(toolCall.getId(), output);
        } catch (Exception e) {
            auditLogManager.createAndAppend(now, toolName, risk.name(), "ERROR_THROWN", toolCall.getArguments());
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
