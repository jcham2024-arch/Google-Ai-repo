package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingActionsFlow(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingActionsList(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAction(action: PendingActionEntity)

    @Query("UPDATE pending_actions SET status = :status WHERE actionId = :actionId")
    suspend fun updateStatus(actionId: String, status: String)

    @Query("DELETE FROM pending_actions WHERE actionId = :actionId")
    suspend fun deleteAction(actionId: String)

    @Query("DELETE FROM pending_actions WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
