package com.apex.core.port;

import com.apex.core.port.model.ApprovalDecision;
import com.apex.core.port.model.ApprovalRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Contract test suite defining the expected lifecycle and behavior for any {@link ApprovalPort} implementation.
 */
public class ApprovalPortContractTest {

    private ApprovalPort approvalPort;
    private final AtomicBoolean approveFlag = new AtomicBoolean(true);

    @Before
    public void setUp() {
        approvalPort = new ApprovalPort() {
            @Override
            public ApprovalDecision requestApproval(ApprovalRequest request) {
                if (approveFlag.get()) {
                    return ApprovalDecision.approve(request.getRequestId(), "Contract approved");
                } else {
                    return ApprovalDecision.deny(request.getRequestId(), "Contract rejected");
                }
            }
        };
    }

    @Test
    public void testApprovalLifecycleApproved() {
        approveFlag.set(true);
        ApprovalRequest request = new ApprovalRequest(
            "contract_req_1",
            "dangerous_op",
            Collections.singletonMap("path", "/etc/hosts"),
            "L3",
            "Write requires human consent"
        );

        ApprovalDecision decision = approvalPort.requestApproval(request);
        assertNotNull(decision);
        assertEquals("contract_req_1", decision.getRequestId());
        assertTrue(decision.isApproved());
        assertEquals("Contract approved", decision.getApproverComment());
    }

    @Test
    public void testApprovalLifecycleDenied() {
        approveFlag.set(false);
        ApprovalRequest request = new ApprovalRequest(
            "contract_req_2",
            "dangerous_op",
            Collections.singletonMap("path", "/etc/hosts"),
            "L3",
            "Write requires human consent"
        );

        ApprovalDecision decision = approvalPort.requestApproval(request);
        assertNotNull(decision);
        assertEquals("contract_req_2", decision.getRequestId());
        assertFalse(decision.isApproved());
        assertEquals("Contract rejected", decision.getApproverComment());
    }
}
