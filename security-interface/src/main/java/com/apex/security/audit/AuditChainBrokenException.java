package com.apex.security.audit;

public class AuditChainBrokenException extends RuntimeException {
    public AuditChainBrokenException(String message) {
        super(message);
    }
}
