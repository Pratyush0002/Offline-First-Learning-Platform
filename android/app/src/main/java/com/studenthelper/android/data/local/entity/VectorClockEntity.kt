package com.studenthelper.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vector_clocks")
data class VectorClockEntity(
    @PrimaryKey
    val clientId: String,
    val highestClock: Long
)
