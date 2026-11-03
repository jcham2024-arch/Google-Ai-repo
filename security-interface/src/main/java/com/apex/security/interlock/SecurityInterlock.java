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
import com.apex.security.policy.RiskLevel;
import com.apex.security.policy.ToolRiskRegistry;

import java.util.Optional;

/**
 * Enforced security interlock boundary implementing {@link ToolExecutorPort}.
 * <p>
 * This class coordinates the safety pipeline for all agentic tool invocations:
 * <ol>
 *   <li>Evaluates the tool's classified {@link RiskLevel} against {@link ToolRiskRegistry}.</li>
 *   <li>If {@code riskLevel >= L3}, suspends execution and queries {@link ApprovalPort} for user consent.</li>
 *   <li>If denied by the user, aborts execution and logs a {@code DENIED} entry into {@link AuditLogManager}.</li>
 *   <li>If approved (or if {@code riskLevel < L3}), dispatches execution to {@link ToolRegistry} and records {@code EXECUTED_OK}.</li>
 *   <li>Cryptographically binds every event into an append-only SHA-256 hash chain.</li>
 * </ol>
 */
public class SecurityInterlock implements ToolExecutorPort {
    private final ToolRegistry toolRegistry;
    private final ToolRiskRegistry riskRegistry;
    private final AuditLogManager auditLogManager;
    private final ApprovalPort approvalPort;

    /**
     * Constructs a {@code SecurityInterlock} instance.
     *
     * @param toolRegistry     underlying registry of executable tool workers.
     * @param riskRegistry     policy registry classifying tool risk levels.
     * @param auditLogManager  tamper-evident cryptographic audit manager.
     * @param approvalPort     UI callback gate for human authorization prompts.
     */
    public SecurityInterlock(ToolRegistry toolRegistry, ToolRiskRegistry riskRegistry,
                             AuditLogManager auditLogManager, ApprovalPort approvalPort) {
        this.toolRegistry = toolRegistry;
        this.riskRegistry = riskRegistry != null ? riskRegistry : new ToolRiskRegistry();
        this.auditLogManager = auditLogManager != null ? auditLogManager : new AuditLogManager();
        this.approvalPort = approvalPort;
    }

    /**
     * Evaluates and executes a tool call subject to risk classification and human approval.
     *
     * @param toolCall target tool identifier and parameter map.
     * @return an {@link ExecutionResult} denoting output text or structured denial error.
     */
    @Override
    public ExecutionResult execute(ToolCall toolCall) {
        String toolName = toolCall.getToolName();
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
            ApprovalDecision decision = approvalPort.requestApproval(request);

            if (!decision.isApproved()) {
                auditLogManager.createAndAppend(now, toolName, risk.name(), "DENIED", toolCall.getArguments().toString());
                return ExecutionResult.fail(toolCall.getId(), "Tool execution denied by user: " + decision.getApproverComment());
            }
        }

        // Execute tool
        Optional<ToolWorker> workerOpt = toolRegistry.getWorker(toolName);
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

    /**
     * Returns the active audit manager holding the tamper-evident log.
     */
    public AuditLogManager getAuditLogManager() {
        return auditLogManager;
    }

    /**
     * Returns the tool risk policy registry.
     */
    public ToolRiskRegistry getRiskRegistry() {
        return riskRegistry;
    }
}
