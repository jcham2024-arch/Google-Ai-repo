package com.apex.core.port;

import com.apex.core.port.model.AuditRecord;
import java.util.List;

public interface AuditPort {
    void append(AuditRecord record);
    List<AuditRecord> getTrail();
    boolean verifyChain();
}
