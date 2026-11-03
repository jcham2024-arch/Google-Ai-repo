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

public class SecurityInterlock implements ToolExecutorPort {
    private final ToolRegistry toolRegistry;
    private final ToolRiskRegistry riskRegistry;
    private final AuditLogManager auditLogManager;
    private final ApprovalPort approvalPort;

    public SecurityInterlock(ToolRegistry toolRegistry, ToolRiskRegistry riskRegistry,
                             AuditLogManager auditLogManager, ApprovalPort approvalPort) {
        this.toolRegistry = toolRegistry;
        this.riskRegistry = riskRegistry != null ? riskRegistry : new ToolRiskRegistry();
        this.auditLogManager = auditLogManager != null ? auditLogManager : new AuditLogManager();
        this.approvalPort = approvalPort;
    }

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

    public AuditLogManager getAuditLogManager() {
        return auditLogManager;
    }

    public ToolRiskRegistry getRiskRegistry() {
        return riskRegistry;
    }
}
