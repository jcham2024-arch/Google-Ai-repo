package com.apex.security.policy;

import com.apex.core.port.ApprovalPort;
import com.apex.core.port.model.ApprovalDecision;
import com.apex.core.port.model.ExecutionResult;
import com.apex.core.port.model.ToolCall;
import com.apex.core.tool.ToolRegistry;
import com.apex.core.tool.ToolWorker;
import com.apex.security.audit.AuditLogManager;
import com.apex.security.interlock.SecurityInterlock;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying security interlock risk gating, fail-closed defaults, and approval workflows.
 */
public class SecurityInterlockTest {

    private ToolRegistry toolRegistry;
    private ToolRiskRegistry riskRegistry;
    private AuditLogManager auditLogManager;
    private AtomicBoolean approvalState;
    private ApprovalPort approvalPort;

    @Before
    public void setUp() {
        toolRegistry = new ToolRegistry();
        riskRegistry = new ToolRiskRegistry();
        auditLogManager = new AuditLogManager();
        approvalState = new AtomicBoolean(true);

        approvalPort = request -> {
            if (approvalState.get()) {
                return ApprovalDecision.approve(request.getRequestId(), "User confirmed");
            } else {
                return ApprovalDecision.deny(request.getRequestId(), "User rejected");
            }
        };

        // Register sample tool worker
        toolRegistry.register(new ToolWorker() {
            @Override
            public String getName() {
                return "read_file";
            }

            @Override
            public String getDescription() {
                return "Reads file";
            }

            @Override
            public String execute(Map<String, String> arguments) {
                return "file content";
            }
        });

        toolRegistry.register(new ToolWorker() {
            @Override
            public String getName() {
                return "run_command";
            }

            @Override
            public String getDescription() {
                return "Runs system command";
            }

            @Override
            public String execute(Map<String, String> arguments) {
                return "command executed";
            }
        });

        toolRegistry.register(new ToolWorker() {
            @Override
            public String getName() {
                return "failing_tool";
            }

            @Override
            public String getDescription() {
                return "Always fails";
            }

            @Override
            public String execute(Map<String, String> arguments) throws Exception {
                throw new IllegalStateException("Execution crashed");
            }
        });
    }

    @Test
    public void testL0ToolExecutesWithoutApproval() {
        // read_file is mapped to L0
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null);
        ToolCall call = new ToolCall("c1", "read_file", Collections.singletonMap("path", "/test.txt"));

        ExecutionResult result = interlock.execute(call);
        assertTrue(result.isSuccess());
        assertEquals("file content", result.getOutput());
        assertEquals(1, auditLogManager.getTrail().size());
        assertEquals("EXECUTED_OK", auditLogManager.getTrail().get(0).getDecision());
    }

    @Test
    public void testL4ToolRequiresApprovalAndExecutesWhenApproved() {
        // run_command is L4 (requires approval)
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, approvalPort);
        approvalState.set(true);

        ToolCall call = new ToolCall("c2", "run_command", Collections.singletonMap("cmd", "uptime"));
        ExecutionResult result = interlock.execute(call);

        assertTrue(result.isSuccess());
        assertEquals("command executed", result.getOutput());
        assertEquals(1, auditLogManager.getTrail().size());
        assertEquals("EXECUTED_OK", auditLogManager.getTrail().get(0).getDecision());
    }

    @Test
    public void testL4ToolFailsWhenUserRejects() {
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, approvalPort);
        approvalState.set(false); // deny

        ToolCall call = new ToolCall("c3", "run_command", Collections.singletonMap("cmd", "rm -rf /"));
        ExecutionResult result = interlock.execute(call);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Tool execution denied by user"));
        assertEquals(1, auditLogManager.getTrail().size());
        assertEquals("DENIED", auditLogManager.getTrail().get(0).getDecision());
    }

    @Test
    public void testUnknownToolFailsClosedToL3() {
        // Unregistered tool defaults to L3 in ToolRiskRegistry
        assertEquals(RiskLevel.L3, riskRegistry.getRiskLevel("unregistered_tool"));

        // If no approver is configured, it must fail closed
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null);
        ToolCall call = new ToolCall("c4", "unregistered_tool", Collections.emptyMap());

        ExecutionResult result = interlock.execute(call);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("no ApprovalPort configured"));
        assertEquals(1, auditLogManager.getTrail().size());
        assertEquals("DENIED_NO_APPROVER", auditLogManager.getTrail().get(0).getDecision());
    }

    @Test
    public void testToolExceptionHandledGracefully() {
        riskRegistry.registerRisk("failing_tool", RiskLevel.L0);
        SecurityInterlock interlock = new SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null);

        ToolCall call = new ToolCall("c5", "failing_tool", Collections.emptyMap());
        ExecutionResult result = interlock.execute(call);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Execution crashed"));
        assertEquals(1, auditLogManager.getTrail().size());
        assertEquals("ERROR_THROWN", auditLogManager.getTrail().get(0).getDecision());
    }
}
