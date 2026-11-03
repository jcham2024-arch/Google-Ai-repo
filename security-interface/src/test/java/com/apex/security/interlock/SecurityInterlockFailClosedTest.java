package com.apex.security.interlock;

import com.apex.core.port.ApprovalPort;
import com.apex.core.port.model.ApprovalDecision;
import com.apex.core.port.model.ExecutionResult;
import com.apex.core.port.model.ToolCall;
import com.apex.core.tool.ToolRegistry;
import com.apex.security.audit.AuditLogManager;
import com.apex.security.policy.BudgetExhaustedException;
import com.apex.security.policy.ToolRiskRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Parameterized and focused unit tests verifying fail-closed interlock behavior across all edge cases.
 */
public class SecurityInterlockFailClosedTest {

    private ToolRegistry toolRegistry;
    private ToolRiskRegistry riskRegistry;
    private AuditLogManager auditLogManager;

    @Before
    public void setUp() {
        toolRegistry = new ToolRegistry();
        riskRegistry = new ToolRiskRegistry();
        auditLogManager = new AuditLogManager();
    }

    @Test
    public void testNullToolCallIsDenied() {
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null);
        ExecutionResult res = interlock.execute(null);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorMessage().contains("Security denial"));
    }

    @Test
    public void testThrowingApprovalPortIsDenied() {
        ApprovalPort throwingPort = request -> {
            throw new RuntimeException("UI crashed!");
        };

        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, throwingPort);
        ToolCall call = new ToolCall("c1", "run_command", Collections.emptyMap());

        ExecutionResult res = interlock.execute(call);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorMessage().contains("ApprovalPort threw exception"));
    }

    @Test
    public void testNullDecisionFromApprovalPortIsDenied() {
        ApprovalPort nullPort = request -> null;

        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, nullPort);
        ToolCall call = new ToolCall("c2", "run_command", Collections.emptyMap());

        ExecutionResult res = interlock.execute(call);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorMessage().contains("denied by user"));
    }

    @Test
    public void testMismatchedRequestIdIsDenied() {
        ApprovalPort mismatchedPort = request -> ApprovalDecision.approve("different_req_id", "OK");

        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, mismatchedPort);
        ToolCall call = new ToolCall("c3", "run_command", Collections.emptyMap());

        ExecutionResult res = interlock.execute(call);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorMessage().contains("request ID mismatch"));
    }

    @Test
    public void testExceedingMaxIterationsThrowsBudgetExhaustedException() {
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null, 2);

        ToolCall call = new ToolCall("c4", "read_file", Collections.emptyMap());
        interlock.execute(call); // iteration 1
        interlock.execute(call); // iteration 2

        try {
            interlock.execute(call); // iteration 3 -> exceeds 2
            fail("Expected BudgetExhaustedException");
        } catch (BudgetExhaustedException expected) {
            assertTrue(expected.getMessage().contains("max iterations"));
        }
    }
}
