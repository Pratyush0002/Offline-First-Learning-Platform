package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey
    val id: String, // studentId + ":" + lessonId
    val lessonId: String,
    val studentId: String,
    val progressPercentage: Int,
    val isCompleted: Boolean,
    val quizScore: Int,
    val lastReadBlockId: String?,
    val lamportClock: Long,
    val clientId: String,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
