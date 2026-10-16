package com.studenthelper.android.data.local.dao

import androidx.room.*
import com.studenthelper.android.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE lessonId = :lessonId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getNotesByLesson(lessonId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteDirect(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, deleteClock = :deleteClock, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, deleteClock: Long, timestamp: Long = System.currentTimeMillis())
}
