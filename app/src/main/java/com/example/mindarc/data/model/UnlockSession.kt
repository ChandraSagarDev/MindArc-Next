package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_sessions")
data class UnlockSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityRecordId: Long,
    val startTime: Long,
    val endTime: Long,
    val isActive: Boolean = true
)

