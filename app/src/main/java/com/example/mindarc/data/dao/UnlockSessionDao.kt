package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mindarc.data.model.UnlockSession
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockSessionDao {
    @Query("SELECT * FROM unlock_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): UnlockSession?

    @Query("SELECT * FROM unlock_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<UnlockSession>>

    @Query("SELECT * FROM unlock_sessions WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<UnlockSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UnlockSession): Long

    @Update
    suspend fun updateSession(session: UnlockSession)

    @Query("UPDATE unlock_sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSessions()
}

