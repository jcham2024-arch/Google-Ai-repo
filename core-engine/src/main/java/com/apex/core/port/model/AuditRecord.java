package com.apex.core.port.model;

import java.util.Objects;

public final class AuditRecord {
    private final long timestamp;
    private final String toolName;
    private final String riskLevel;
    private final String decision;
    private final String argumentsHash;
    private final String previousHash;
    private final String entryHash;

    public AuditRecord(long timestamp, String toolName, String riskLevel, String decision,
                       String argumentsHash, String previousHash, String entryHash) {
        this.timestamp = timestamp;
        this.toolName = toolName != null ? toolName : "";
        this.riskLevel = riskLevel != null ? riskLevel : "";
        this.decision = decision != null ? decision : "";
        this.argumentsHash = argumentsHash != null ? argumentsHash : "";
        this.previousHash = previousHash != null ? previousHash : "";
        this.entryHash = entryHash != null ? entryHash : "";
    }

    public long getTimestamp() { return timestamp; }
    public String getToolName() { return toolName; }
    public String getRiskLevel() { return riskLevel; }
    public String getDecision() { return decision; }
    public String getArgumentsHash() { return argumentsHash; }
    public String getPreviousHash() { return previousHash; }
    public String getEntryHash() { return entryHash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditRecord that = (AuditRecord) o;
        return timestamp == that.timestamp &&
               Objects.equals(toolName, that.toolName) &&
               Objects.equals(riskLevel, that.riskLevel) &&
               Objects.equals(decision, that.decision) &&
               Objects.equals(argumentsHash, that.argumentsHash) &&
               Objects.equals(previousHash, that.previousHash) &&
               Objects.equals(entryHash, that.entryHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, toolName, riskLevel, decision, argumentsHash, previousHash, entryHash);
    }

    @Override
    public String toString() {
        return "AuditRecord{tool='" + toolName + "', decision='" + decision + "', hash='" + entryHash + "'}";
    }
}
