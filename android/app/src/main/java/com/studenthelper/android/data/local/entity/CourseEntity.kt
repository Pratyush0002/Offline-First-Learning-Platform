package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val level: String,
    val thumbnailUrl: String?,
    val estimatedHours: Int,
    val isDownloadedOffline: Boolean = false,
    val downloadedAt: Long = 0L
)
