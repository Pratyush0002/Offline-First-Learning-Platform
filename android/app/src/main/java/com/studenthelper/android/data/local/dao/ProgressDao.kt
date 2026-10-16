package com.studenthelper.android.data.local.dao

import androidx.room.*
import com.studenthelper.android.data.local.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId AND studentId = :studentId")
    fun getProgress(lessonId: String, studentId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId AND studentId = :studentId")
    suspend fun getProgressDirect(lessonId: String, studentId: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_progress WHERE studentId = :studentId")
    fun getAllProgressForStudent(studentId: String): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LessonProgressEntity)

    @Query("SELECT * FROM lesson_progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<LessonProgressEntity>

    @Query("UPDATE lesson_progress SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markProgressSynced(ids: List<String>)
}
