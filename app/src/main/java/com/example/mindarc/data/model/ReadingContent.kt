package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_content")
data class ReadingContent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val estimatedReadingTimeMinutes: Int,
    val category: String? = null
)

