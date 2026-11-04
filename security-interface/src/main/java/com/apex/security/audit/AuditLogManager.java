package com.apex.security.audit;

import com.apex.core.port.AuditPort;
import com.apex.core.port.AuditStorePort;
import com.apex.core.port.model.AuditRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Audit log manager implementing {@link AuditPort} and delegating persistence to an {@link AuditStorePort}.
 */
public class AuditLogManager implements AuditPort {
    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditStorePort store;
    private final List<AuditRecord> inMemoryFallback = new ArrayList<>();
    private String lastHash = GENESIS_HASH;

    public AuditLogManager() {
        this(null);
    }

    public AuditLogManager(AuditStorePort store) {
        this.store = store;
        if (store != null) {
            List<AuditRecord> existing = store.readAll();
            if (!existing.isEmpty()) {
                this.lastHash = existing.get(existing.size() - 1).getEntryHash();
            }
        }
    }

    @Override
    public synchronized void append(AuditRecord record) {
        if (record != null) {
            if (store != null) {
                store.append(record);
            } else {
                inMemoryFallback.add(record);
            }
            lastHash = record.getEntryHash();
        }
    }

    /**
     * Canonicalizes a map to a deterministic JSON string by sorting keys recursively.
     */
    private static String canonicalizeArguments(Map<String, String> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        // Use TreeMap to ensure sorted keys
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return toCanonicalJson(sorted);
    }

    private static String toCanonicalJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return escapeJson((String) value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            // Use TreeMap to ensure sorted keys recursively
            Map<String, Object> sortedMap = new TreeMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(escapeJson(entry.getKey())).append(":");
                sb.append(toCanonicalJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toCanonicalJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        // Fallback: treat as string
        return escapeJson(value.toString());
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public synchronized AuditRecord createAndAppend(long timestamp, String toolName, String riskLevel,
                                                    String decision, Map<String, String> arguments) {
        String canonicalArgs = canonicalizeArguments(arguments);
        String argsHash = sha256(canonicalArgs);
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

    public synchronized AuditRecord createAndAppend(long timestamp, String toolName, String riskLevel,
                                                    String decision, String reason) {
        Map<String, String> args = Collections.singletonMap("reason", reason != null ? reason : "");
        return createAndAppend(timestamp, toolName, riskLevel, decision, args);
    }

    @Override
    public synchronized List<AuditRecord> getTrail() {
        if (store != null) {
            return store.readAll();
        }
        return Collections.unmodifiableList(new ArrayList<>(inMemoryFallback));
    }

    @Override
    public synchronized boolean verifyChain() {
        if (store != null) {
            return store.verify() == null;
        }
        String expectedPrev = GENESIS_HASH;
        for (AuditRecord record : inMemoryFallback) {
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
