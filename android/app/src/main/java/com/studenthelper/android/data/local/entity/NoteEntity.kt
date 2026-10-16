package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["lessonId"])]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val lessonId: String?,
    val authorId: String,
    val title: String,
    val titleClock: Long,
    val titleClientId: String,
    val contentMarkdown: String,
    val isDeleted: Boolean = false,
    val deleteClock: Long = 0L,
    val tagsJson: String = "[]",
    val crdtStateJson: String? = null,
    val isSyncedWithServer: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
