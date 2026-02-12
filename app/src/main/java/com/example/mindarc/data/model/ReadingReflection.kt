package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class ReadingReflection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityRecordId: Long,
    val question: String,
    val answer: String
)
