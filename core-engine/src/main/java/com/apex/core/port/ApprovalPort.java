package com.apex.core.port;

import com.apex.core.port.model.ApprovalRequest;
import com.apex.core.port.model.ApprovalDecision;

public interface ApprovalPort {
    ApprovalDecision requestApproval(ApprovalRequest request);
}
