package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mindarc.data.model.ReadingContent
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingContentDao {
    @Query("SELECT * FROM reading_content")
    fun getAllContent(): Flow<List<ReadingContent>>

    @Query("SELECT * FROM reading_content WHERE id = :id")
    suspend fun getContentById(id: Long): ReadingContent?

    @Query("SELECT * FROM reading_content ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomContent(): ReadingContent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: ReadingContent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(contents: List<ReadingContent>)
}

