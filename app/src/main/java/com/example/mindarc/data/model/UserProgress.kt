package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val id: Int = 1, // Singleton
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: Long? = null,
    val totalActivities: Int = 0,
    val totalUnlockSessions: Int = 0,
    val perfectScoreStreak: Int = 0,
    val multiplierEndTime: Long = 0,
    val earnedBadges: Set<Badge> = emptySet(),
    val completedCategories: Set<String> = emptySet()
)
