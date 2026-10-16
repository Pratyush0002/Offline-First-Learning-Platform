package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lessons",
    indices = [Index(value = ["courseId"])]
)
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val courseId: String,
    val title: String,
    val orderIndex: Int,
    val summary: String,
    val contentMarkdown: String,
    val estimatedMinutes: Int,
    val quizQuestionsJson: String?,
    val resourcesJson: String?,
    val isDownloadedOffline: Boolean = false
)
