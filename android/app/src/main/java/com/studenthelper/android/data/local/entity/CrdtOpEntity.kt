package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crdt_operations",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["isSynced"])
    ]
)
data class CrdtOpEntity(
    @PrimaryKey
    val opId: String,
    val noteId: String,
    val type: String,
    val clientId: String,
    val lamportClock: Long,
    val timestamp: Long,
    val targetId: String?,
    val payload: String?,
    val secondaryPayload: String?,
    val isSynced: Boolean = false // false = pending sync in offline outbox
)
