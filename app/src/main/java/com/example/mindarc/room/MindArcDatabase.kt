package com.example.mindarc.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mindarc.room.dao.BlockRuleDao
import com.example.mindarc.room.entity.BlockRule

@Database(entities = [BlockRule::class], version = 1)
abstract class MindArcDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
}
