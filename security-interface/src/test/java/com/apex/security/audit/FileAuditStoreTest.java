package com.apex.security.audit;

import com.apex.core.port.model.AuditRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests verifying durable file persistence, hash-chain integrity, tampering detection, and rotation.
 */
public class FileAuditStoreTest {

    private File tempDir;
    private FileAuditStore store;

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "apex_audit_test_" + System.nanoTime());
        tempDir.mkdirs();
        store = new FileAuditStore(tempDir);
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
        }
        deleteRecursively(tempDir);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private AuditRecord makeRecord(long ts, String tool, String risk, String decision, String args, String prevHash) {
        String argsHash = FileAuditStore.sha256(args);
        String raw = ts + ":" + tool + ":" + risk + ":" + decision + ":" + argsHash + ":" + prevHash;
        String entryHash = FileAuditStore.sha256(raw);
        return new AuditRecord(ts, tool, risk, decision, argsHash, prevHash, entryHash);
    }

    @Test
    public void testAppendAndReadAll() {
        AuditRecord rec1 = makeRecord(1000L, "read_file", "L0", "EXECUTED_OK", "args1", FileAuditStore.GENESIS_HASH);
        long seq1 = store.append(rec1);
        assertEquals(0L, seq1);

        AuditRecord rec2 = makeRecord(1001L, "write_file", "L1", "EXECUTED_OK", "args2", rec1.getEntryHash());
        long seq2 = store.append(rec2);
        assertEquals(1L, seq2);

        List<AuditRecord> records = store.readAll();
        assertEquals(2, records.size());
        assertEquals("read_file", records.get(0).getToolName());
        assertEquals("write_file", records.get(1).getToolName());

        assertNull(store.verify()); // intact
    }

    @Test
    public void testStateRecoveryAfterReopening() {
        AuditRecord rec1 = makeRecord(1000L, "read_file", "L0", "EXECUTED_OK", "args1", FileAuditStore.GENESIS_HASH);
        store.append(rec1);
        store.close();

        // Re-open store pointing to same directory
        FileAuditStore recovered = new FileAuditStore(tempDir);
        try {
            List<AuditRecord> records = recovered.readAll();
            assertEquals(1, records.size());
            assertEquals(rec1.getEntryHash(), records.get(0).getEntryHash());

            // Next append must link to rec1.entryHash
            AuditRecord rec2 = makeRecord(1002L, "run_command", "L4", "EXECUTED_OK", "args2", rec1.getEntryHash());
            long seq2 = recovered.append(rec2);
            assertEquals(1L, seq2);

            assertNull(recovered.verify());
        } finally {
            recovered.close();
        }
    }

    @Test
    public void testMismatchedPrevHashThrowsException() {
        AuditRecord rec1 = makeRecord(1000L, "read_file", "L0", "EXECUTED_OK", "args1", "bad_genesis_hash");
        try {
            store.append(rec1);
            fail("Expected AuditChainBrokenException");
        } catch (AuditChainBrokenException expected) {
            assertTrue(expected.getMessage().contains("Chain broken"));
        }
    }

    @Test
    public void testTamperingDetection() throws Exception {
        AuditRecord rec1 = makeRecord(1000L, "read_file", "L0", "EXECUTED_OK", "args1", FileAuditStore.GENESIS_HASH);
        store.append(rec1);
        AuditRecord rec2 = makeRecord(1001L, "write_file", "L1", "EXECUTED_OK", "args2", rec1.getEntryHash());
        store.append(rec2);
        store.close();

        // Tamper with active audit file
        File activeFile = new File(tempDir, "audit_active.jsonl");
        String tamperedContent = "{\"seq\":0,\"ts\":1000,\"tool\":\"read_file\",\"risk\":\"L0\",\"decision\":\"TAMPERED\",\"argsHash\":\"000\",\"prev\":\"" +
            FileAuditStore.GENESIS_HASH + "\",\"hash\":\"fake_hash\"}\n";

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(activeFile, false), StandardCharsets.UTF_8)) {
            writer.write(tamperedContent);
        }

        FileAuditStore store2 = new FileAuditStore(tempDir);
        try {
            Long brokenIndex = store2.verify();
            assertNotNull(brokenIndex);
            assertEquals(Long.valueOf(0), brokenIndex);
        } finally {
            store2.close();
        }
    }

    @Test
    public void testRotation() {
        AuditRecord rec1 = makeRecord(1000L, "read_file", "L0", "EXECUTED_OK", "args1", FileAuditStore.GENESIS_HASH);
        store.append(rec1);

        store.rotate();

        // Active file should now be empty and ready for new genesis or chain continuity
        File activeFile = new File(tempDir, "audit_active.jsonl");
        assertTrue(activeFile.exists());

        // Check that archived file was created
        File[] files = tempDir.listFiles((dir, name) -> name.startsWith("audit_") && !name.equals("audit_active.jsonl"));
        assertNotNull(files);
        assertTrue(files.length >= 1);
    }
}
