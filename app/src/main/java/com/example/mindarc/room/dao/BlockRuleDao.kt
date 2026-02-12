package com.example.mindarc.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.mindarc.room.entity.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rules")
    fun getAll(): Flow<List<BlockRule>>
}
