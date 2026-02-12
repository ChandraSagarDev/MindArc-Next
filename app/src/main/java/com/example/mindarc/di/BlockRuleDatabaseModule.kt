package com.example.mindarc.di

import android.content.Context
import androidx.room.Room
import com.example.mindarc.room.MindArcDatabase
import com.example.mindarc.room.dao.BlockRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BlockRuleDatabaseModule {

    @Provides
    @Singleton
    fun provideBlockRuleDatabase(@ApplicationContext context: Context): MindArcDatabase {
        return Room.databaseBuilder(
            context,
            MindArcDatabase::class.java,
            "block_rule.db"
        ).build()
    }

    @Provides
    fun provideBlockRuleDao(database: MindArcDatabase): BlockRuleDao {
        return database.blockRuleDao()
    }
}
