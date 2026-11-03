package com.apex.core.port;

import com.apex.core.port.model.AuditRecord;
import java.util.List;

/**
 * Port abstraction for persistent, append-only, tamper-evident audit storage.
 * <p>
 * Ensures that audit trails persist across application and device restarts and can be
 * verified from genesis for chain integrity.
 */
public interface AuditStorePort {
    /**
     * Appends an audit record to the persistent store.
     *
     * @param record the audit record to store.
     * @return the sequential index or timestamp assigned to this record.
     */
    long append(AuditRecord record);

    /**
     * Reads all audit records in sequential order, oldest first.
     *
     * @return list of audit records.
     */
    List<AuditRecord> readAll();

    /**
     * Verifies the cryptographic hash-chain integrity from genesis.
     *
     * @return null if the chain is intact, or the index/timestamp of the first corrupted record.
     */
    Long verify();

    /**
     * Closes the store and forces any cached buffers to persistent storage.
     */
    void close();
}
