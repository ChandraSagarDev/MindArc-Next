package com.example.mindarc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.mindarc.data.model.ReadingReflection

@Dao
interface ReadingReflectionDao {
    @Insert
    suspend fun insertReflection(reflection: ReadingReflection)
}
