package com.apex.core.port.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying port model immutability, defaults, equality contracts, and null-safety.
 */
public class ChatModelPortModelTest {

    @Test
    public void testChatRequestContracts() {
        ChatRequest req1 = new ChatRequest("Hello", "Be helpful", "deepseek-chat", Arrays.asList("tool1", "tool2"));
        assertEquals("Hello", req1.getPrompt());
        assertEquals("Be helpful", req1.getSystemInstruction());
        assertEquals("deepseek-chat", req1.getModel());
        assertEquals(2, req1.getAvailableTools().size());

        ChatRequest req2 = new ChatRequest("Hello", "Be helpful", "deepseek-chat", Arrays.asList("tool1", "tool2"));
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertTrue(req1.toString().contains("Hello"));

        // Null safety
        ChatRequest nullReq = new ChatRequest(null, null, null, null);
        assertEquals("", nullReq.getPrompt());
        assertEquals("", nullReq.getSystemInstruction());
        assertEquals("default-model", nullReq.getModel());
        assertTrue(nullReq.getAvailableTools().isEmpty());
    }

    @Test
    public void testChatResponseOkAndError() {
        ToolCall tc = new ToolCall("call_1", "calc", Collections.singletonMap("expr", "2+2"));
        ChatResponse okResp = ChatResponse.ok("4", "gpt-4o", Collections.singletonList(tc));
        assertTrue(okResp.isSuccess());
        assertEquals("4", okResp.getContent());
        assertEquals("gpt-4o", okResp.getModel());
        assertEquals(1, okResp.getToolCalls().size());
        assertNull(okResp.getErrorMessage());

        ChatResponse errResp = ChatResponse.error("Rate limit exceeded", "gpt-4o");
        assertFalse(errResp.isSuccess());
        assertEquals("", errResp.getContent());
        assertEquals("Rate limit exceeded", errResp.getErrorMessage());
        assertTrue(errResp.getToolCalls().isEmpty());
    }

    @Test
    public void testToolCallAndExecutionResult() {
        Map<String, String> args = new HashMap<>();
        args.put("key", "val");
        ToolCall tc1 = new ToolCall("id_1", "tool_a", args);
        ToolCall tc2 = new ToolCall("id_1", "tool_a", args);

        assertEquals("id_1", tc1.getId());
        assertEquals("tool_a", tc1.getToolName());
        assertEquals("val", tc1.getArguments().get("key"));
        assertEquals(tc1, tc2);
        assertEquals(tc1.hashCode(), tc2.hashCode());

        ExecutionResult ok = ExecutionResult.ok("id_1", "Output text");
        assertTrue(ok.isSuccess());
        assertEquals("Output text", ok.getOutput());
        assertNull(ok.getErrorMessage());

        ExecutionResult fail = ExecutionResult.fail("id_1", "Fail msg");
        assertFalse(fail.isSuccess());
        assertEquals("Fail msg", fail.getErrorMessage());
    }

    @Test
    public void testApprovalModels() {
        ApprovalRequest req = new ApprovalRequest(
            "req_99",
            "execute_bash",
            Collections.singletonMap("cmd", "ls -la"),
            "L3",
            "User confirmation needed"
        );
        assertEquals("req_99", req.getRequestId());
        assertEquals("execute_bash", req.getToolName());
        assertEquals("L3", req.getRiskLevel());
        assertEquals("User confirmation needed", req.getReason());

        ApprovalDecision approved = ApprovalDecision.approve("req_99", "Proceed");
        assertTrue(approved.isApproved());
        assertEquals("Proceed", approved.getApproverComment());

        ApprovalDecision denied = ApprovalDecision.deny("req_99", "Risk too high");
        assertFalse(denied.isApproved());
        assertEquals("Risk too high", denied.getApproverComment());
    }

    @Test
    public void testAuditRecordContracts() {
        AuditRecord rec = new AuditRecord(
            1700000000000L,
            "read_file",
            "L1",
            "EXECUTED_OK",
            "args_hash_123",
            "prev_hash_abc",
            "entry_hash_xyz"
        );
        assertEquals(1700000000000L, rec.getTimestamp());
        assertEquals("read_file", rec.getToolName());
        assertEquals("L1", rec.getRiskLevel());
        assertEquals("EXECUTED_OK", rec.getDecision());
        assertEquals("args_hash_123", rec.getArgumentsHash());
        assertEquals("prev_hash_abc", rec.getPreviousHash());
        assertEquals("entry_hash_xyz", rec.getEntryHash());
        assertNotNull(rec.toString());
    }
}
