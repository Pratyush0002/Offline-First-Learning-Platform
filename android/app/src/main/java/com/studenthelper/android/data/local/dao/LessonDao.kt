package com.studenthelper.android.data.local.dao

import androidx.room.*
import com.studenthelper.android.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLessonsByCourse(courseId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    fun getLessonById(id: String): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonDirect(id: String): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET isDownloadedOffline = :downloaded WHERE id = :lessonId")
    suspend fun setDownloadedOffline(lessonId: String, downloaded: Boolean)
}
