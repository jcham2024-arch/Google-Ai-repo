package com.apex.security.audit;

import com.apex.core.port.AuditStorePort;
import com.apex.core.port.model.AuditRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Durable, append-only, tamper-evident audit store writing newline-delimited JSON (JSONL).
 * <p>
 * Security Invariants:
 * <ul>
 *   <li>Every write is flushed to physical storage using {@link FileChannel#force(boolean)}.</li>
 *   <li>Verifies {@code previousHash} matches the preceding record before appending.</li>
 *   <li>Throws {@link AuditChainBrokenException} if tampering is detected.</li>
 * </ul>
 */
public class FileAuditStore implements AuditStorePort {
    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final File directory;
    private File activeFile;
    private FileChannel channel;
    private String lastHash = GENESIS_HASH;
    private long currentSequence = 0;

    public FileAuditStore(File directory) {
        this.directory = directory;
        if (!directory.exists()) {
            directory.mkdirs();
        }
        initOrRecover();
    }

    private synchronized void initOrRecover() {
        this.activeFile = new File(directory, "audit_active.jsonl");
        try {
            boolean isNew = !activeFile.exists();
            this.channel = FileChannel.open(
                activeFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );

            if (!isNew) {
                List<AuditRecord> existing = readAll();
                if (!existing.isEmpty()) {
                    AuditRecord last = existing.get(existing.size() - 1);
                    this.lastHash = last.getEntryHash();
                    this.currentSequence = existing.size();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open audit file channel: " + activeFile, e);
        }
    }

    @Override
    public synchronized long append(AuditRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Audit record cannot be null");
        }

        // Validate hash link before writing
        if (!record.getPreviousHash().equals(lastHash)) {
            throw new AuditChainBrokenException("Chain broken: expected prevHash " + lastHash + " but got " + record.getPreviousHash());
        }

        String jsonLine = "{\"seq\":" + currentSequence +
            ",\"ts\":" + record.getTimestamp() +
            ",\"tool\":\"" + escapeJson(record.getToolName()) + "\"" +
            ",\"risk\":\"" + escapeJson(record.getRiskLevel()) + "\"" +
            ",\"decision\":\"" + escapeJson(record.getDecision()) + "\"" +
            ",\"argsHash\":\"" + record.getArgumentsHash() + "\"" +
            ",\"prev\":\"" + record.getPreviousHash() + "\"" +
            ",\"hash\":\"" + record.getEntryHash() + "\"}\n";

        try {
            byte[] bytes = jsonLine.getBytes(StandardCharsets.UTF_8);
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true); // fsync
            lastHash = record.getEntryHash();
            return currentSequence++;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to audit file", e);
        }
    }

    @Override
    public synchronized List<AuditRecord> readAll() {
        if (!activeFile.exists()) {
            return Collections.emptyList();
        }
        List<AuditRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(activeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                AuditRecord record = parseRecord(line);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read audit records", e);
        }
        return Collections.unmodifiableList(records);
    }

    @Override
    public synchronized Long verify() {
        List<AuditRecord> records = readAll();
        String expectedPrev = GENESIS_HASH;

        for (int i = 0; i < records.size(); i++) {
            AuditRecord r = records.get(i);
            if (!r.getPreviousHash().equals(expectedPrev)) {
                return (long) i;
            }
            String raw = r.getTimestamp() + ":" + r.getToolName() + ":" +
                r.getRiskLevel() + ":" + r.getDecision() + ":" +
                r.getArgumentsHash() + ":" + r.getPreviousHash();
            String computed = sha256(raw);
            if (!computed.equals(r.getEntryHash())) {
                return (long) i;
            }
            expectedPrev = r.getEntryHash();
        }
        return null; // intact
    }

    public synchronized void rotate() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.force(true);
                channel.close();
            }
            File archived = new File(directory, "audit_" + System.currentTimeMillis() + ".jsonl");
            if (activeFile.exists()) {
                activeFile.renameTo(archived);
            }
            initOrRecover();
        } catch (IOException e) {
            throw new RuntimeException("Failed to rotate audit log", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.force(true);
                channel.close();
            }
        } catch (IOException ignored) {}
    }

    private AuditRecord parseRecord(String line) {
        String tsStr = extractField(line, "ts");
        String tool = extractField(line, "tool");
        String risk = extractField(line, "risk");
        String decision = extractField(line, "decision");
        String argsHash = extractField(line, "argsHash");
        String prev = extractField(line, "prev");
        String hash = extractField(line, "hash");

        long ts = 0;
        try {
            ts = Long.parseLong(tsStr);
        } catch (Exception ignored) {}

        return new AuditRecord(ts, tool, risk, decision, argsHash, prev, hash);
    }

    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\"?([^,\"}]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
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
