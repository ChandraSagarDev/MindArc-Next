package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "activity_records")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityType: ActivityType,
    val pointsEarned: Int,
    val unlockDurationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val readingContentId: Long? = null, // For app-provided reading
    val userReadingTitle: String? = null // For user-provided reading
)

