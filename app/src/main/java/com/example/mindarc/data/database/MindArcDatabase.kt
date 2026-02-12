package com.example.mindarc.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mindarc.data.dao.ActivityRecordDao
import com.example.mindarc.data.dao.QuizQuestionDao
import com.example.mindarc.data.dao.ReadingContentDao
import com.example.mindarc.data.dao.ReadingReflectionDao
import com.example.mindarc.data.dao.RestrictedAppDao
import com.example.mindarc.data.dao.UnlockSessionDao
import com.example.mindarc.data.dao.UserProgressDao
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.model.ReadingContent
import com.example.mindarc.data.model.ReadingReflection
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress

@Database(
    entities = [
        RestrictedApp::class,
        ActivityRecord::class,
        UnlockSession::class,
        UserProgress::class,
        ReadingContent::class,
        QuizQuestion::class,
        ReadingReflection::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MindArcDatabase : RoomDatabase() {
    abstract fun restrictedAppDao(): RestrictedAppDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun unlockSessionDao(): UnlockSessionDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun readingContentDao(): ReadingContentDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun readingReflectionDao(): ReadingReflectionDao
}
