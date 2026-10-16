package com.studenthelper.android.data.local.dao

import androidx.room.*
import com.studenthelper.android.data.local.entity.VectorClockEntity

@Dao
interface VectorClockDao {

    @Query("SELECT * FROM vector_clocks")
    suspend fun getAllClocks(): List<VectorClockEntity>

    @Query("SELECT highestClock FROM vector_clocks WHERE clientId = :clientId")
    suspend fun getClockForClient(clientId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClock(clock: VectorClockEntity)

    @Transaction
    suspend fun updateMaxClock(clientId: String, newClock: Long) {
        val current = getClockForClient(clientId) ?: 0L
        if (newClock > current) {
            upsertClock(VectorClockEntity(clientId, newClock))
        }
    }
}
