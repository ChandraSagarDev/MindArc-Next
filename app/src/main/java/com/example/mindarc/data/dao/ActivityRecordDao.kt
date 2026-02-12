package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Query("SELECT * FROM activity_records ORDER BY completedAt DESC")
    fun getAllActivities(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE completedAt >= :startTime AND completedAt <= :endTime ORDER BY completedAt DESC")
    fun getActivitiesInRange(startTime: Long, endTime: Long): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE activityType = :type ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentActivitiesByType(type: ActivityType, limit: Int = 10): List<ActivityRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityRecord): Long

    @Query("SELECT COUNT(*) FROM activity_records WHERE completedAt >= :startTime AND completedAt <= :endTime")
    suspend fun getActivityCountInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT SUM(pointsEarned) FROM activity_records WHERE completedAt >= :startTime AND completedAt <= :endTime")
    suspend fun getPointsInRange(startTime: Long, endTime: Long): Int?

    @Query("SELECT DISTINCT readingContentId FROM activity_records WHERE readingContentId IS NOT NULL")
    suspend fun getUniqueCategoriesCompleted(): List<Long>
}
