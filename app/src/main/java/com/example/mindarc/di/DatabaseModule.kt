package com.example.mindarc.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.mindarc.data.database.MindArcDatabase
import com.example.mindarc.data.dao.ActivityRecordDao
import com.example.mindarc.data.dao.QuizQuestionDao
import com.example.mindarc.data.dao.ReadingContentDao
import com.example.mindarc.data.dao.ReadingReflectionDao
import com.example.mindarc.data.dao.RestrictedAppDao
import com.example.mindarc.data.dao.UnlockSessionDao
import com.example.mindarc.data.dao.UserProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MindArcDatabase {
        return Room.databaseBuilder(
            context,
            MindArcDatabase::class.java,
            "mindarc_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideRestrictedAppDao(database: MindArcDatabase): RestrictedAppDao {
        return database.restrictedAppDao()
    }

    @Provides
    @Singleton
    fun provideActivityRecordDao(database: MindArcDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }

    @Provides
    @Singleton
    fun provideUnlockSessionDao(database: MindArcDatabase): UnlockSessionDao {
        return database.unlockSessionDao()
    }

    @Provides
    @Singleton
    fun provideUserProgressDao(database: MindArcDatabase): UserProgressDao {
        return database.userProgressDao()
    }

    @Provides
    @Singleton
    fun provideReadingContentDao(database: MindArcDatabase): ReadingContentDao {
        return database.readingContentDao()
    }

    @Provides
    @Singleton
    fun provideQuizQuestionDao(database: MindArcDatabase): QuizQuestionDao {
        return database.quizQuestionDao()
    }

    @Provides
    @Singleton
    fun provideReadingReflectionDao(database: MindArcDatabase): ReadingReflectionDao {
        return database.readingReflectionDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("mindarc_prefs", Context.MODE_PRIVATE)
    }
}
