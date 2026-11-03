package com.apex.security.policy;

public enum RiskLevel {
    L0, L1, L2, L3, L4;

    public boolean requiresApproval() {
        return this.compareTo(L3) >= 0;
    }
}
