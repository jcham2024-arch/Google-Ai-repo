package com.apex.security.audit;

import com.apex.core.port.AuditPort;
import com.apex.core.port.model.AuditRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLogManager implements AuditPort {
    private final List<AuditRecord> trail = new ArrayList<>();
    private String lastHash = "0000000000000000000000000000000000000000000000000000000000000000";

    @Override
    public synchronized void append(AuditRecord record) {
        if (record != null) {
            trail.add(record);
            lastHash = record.getEntryHash();
        }
    }

    public synchronized AuditRecord createAndAppend(long timestamp, String toolName, String riskLevel,
                                                    String decision, String arguments) {
        String argsHash = sha256(arguments != null ? arguments : "");
        String raw = timestamp + ":" + toolName + ":" + riskLevel + ":" + decision + ":" + argsHash + ":" + lastHash;
        String entryHash = sha256(raw);

        AuditRecord record = new AuditRecord(
            timestamp,
            toolName,
            riskLevel,
            decision,
            argsHash,
            lastHash,
            entryHash
        );
        append(record);
        return record;
    }

    @Override
    public synchronized List<AuditRecord> getTrail() {
        return Collections.unmodifiableList(new ArrayList<>(trail));
    }

    @Override
    public synchronized boolean verifyChain() {
        String expectedPrev = "0000000000000000000000000000000000000000000000000000000000000000";
        for (AuditRecord record : trail) {
            if (!record.getPreviousHash().equals(expectedPrev)) {
                return false;
            }
            String raw = record.getTimestamp() + ":" + record.getToolName() + ":" +
                         record.getRiskLevel() + ":" + record.getDecision() + ":" +
                         record.getArgumentsHash() + ":" + record.getPreviousHash();
            String computed = sha256(raw);
            if (!computed.equals(record.getEntryHash())) {
                return false;
            }
            expectedPrev = record.getEntryHash();
        }
        return true;
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
