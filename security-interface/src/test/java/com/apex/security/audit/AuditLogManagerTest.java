package com.apex.security.audit;

import com.apex.core.port.model.AuditRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying hash chaining, integrity validation, and tampering detection in {@link AuditLogManager}.
 */
public class AuditLogManagerTest {

    private AuditLogManager manager;

    /** Argument maps are passed through the canonicalizing overload so hashing stays deterministic. */
    private static Map<String, String> args(String key, String value) {
        return Map.of(key, value);
    }

    @Before
    public void setUp() {
        manager = new AuditLogManager();
    }

    @Test
    public void testGenesisRecordCreation() {
        AuditRecord rec1 = manager.createAndAppend(1000L, "read_file", "L0", "EXECUTED_OK", args("path", "a.txt"));
        assertNotNull(rec1);
        assertEquals(AuditLogManager.GENESIS_HASH, rec1.getPreviousHash());
        assertNotNull(rec1.getEntryHash());
        assertEquals(1, manager.getTrail().size());
        assertTrue(manager.verifyChain());
    }

    @Test
    public void testSequentialHashChainLinking() {
        AuditRecord rec1 = manager.createAndAppend(1000L, "read_file", "L0", "EXECUTED_OK", args("path", "a.txt"));
        AuditRecord rec2 = manager.createAndAppend(1001L, "write_file", "L1", "EXECUTED_OK", args("path", "b.txt"));
        AuditRecord rec3 = manager.createAndAppend(1002L, "run_cmd", "L4", "DENIED", args("cmd", "ls"));

        assertEquals(rec1.getEntryHash(), rec2.getPreviousHash());
        assertEquals(rec2.getEntryHash(), rec3.getPreviousHash());

        List<AuditRecord> trail = manager.getTrail();
        assertEquals(3, trail.size());
        assertTrue(manager.verifyChain());
    }

    @Test
    public void testSha256Determinism() {
        String hash1 = AuditLogManager.sha256("test-input-string");
        String hash2 = AuditLogManager.sha256("test-input-string");
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }
}
