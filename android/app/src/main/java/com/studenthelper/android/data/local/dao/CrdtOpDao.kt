package com.studenthelper.android.data.local.dao

import androidx.room.*
import com.studenthelper.android.data.local.entity.CrdtOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrdtOpDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOp(op: CrdtOpEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOps(ops: List<CrdtOpEntity>)

    @Query("SELECT * FROM crdt_operations WHERE isSynced = 0 ORDER BY lamportClock ASC")
    suspend fun getPendingUnsyncedOps(): List<CrdtOpEntity>

    @Query("SELECT COUNT(*) FROM crdt_operations WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("UPDATE crdt_operations SET isSynced = 1 WHERE opId IN (:opIds)")
    suspend fun markOpsSynced(opIds: List<String>)

    @Query("SELECT * FROM crdt_operations WHERE noteId = :noteId ORDER BY lamportClock ASC, clientId ASC")
    suspend fun getOpsForNote(noteId: String): List<CrdtOpEntity>

    @Query("SELECT MAX(lamportClock) FROM crdt_operations WHERE clientId = :clientId")
    suspend fun getMaxClockForClient(clientId: String): Long?
}
