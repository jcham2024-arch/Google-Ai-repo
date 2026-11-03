package com.apex.core.port.model;

import java.util.Objects;

public final class ApprovalDecision {
    private final String requestId;
    private final boolean approved;
    private final String approverComment;

    public ApprovalDecision(String requestId, boolean approved, String approverComment) {
        this.requestId = requestId != null ? requestId : "";
        this.approved = approved;
        this.approverComment = approverComment != null ? approverComment : "";
    }

    public static ApprovalDecision approve(String requestId, String comment) {
        return new ApprovalDecision(requestId, true, comment);
    }

    public static ApprovalDecision deny(String requestId, String reason) {
        return new ApprovalDecision(requestId, false, reason);
    }

    public String getRequestId() { return requestId; }
    public boolean isApproved() { return approved; }
    public String getApproverComment() { return approverComment; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalDecision that = (ApprovalDecision) o;
        return approved == that.approved &&
               Objects.equals(requestId, that.requestId) &&
               Objects.equals(approverComment, that.approverComment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, approved, approverComment);
    }

    @Override
    public String toString() {
        return "ApprovalDecision{approved=" + approved + ", comment='" + approverComment + "'}";
    }
}
