package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_reflections",
    foreignKeys = [
        ForeignKey(
            entity = ActivityRecord::class,
            parentColumns = ["id"],
            childColumns = ["activityRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["activityRecordId"])]
)
data class ReadingReflection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityRecordId: Long,
    val question: String,
    val answer: String
)
