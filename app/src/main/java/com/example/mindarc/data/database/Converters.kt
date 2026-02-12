package com.example.mindarc.data.database

import androidx.room.TypeConverter
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.Badge

class Converters {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String {
        return value.name
    }

    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return ActivityType.valueOf(value)
    }

    @TypeConverter
    fun fromBadgeSet(badges: Set<Badge>): String {
        return badges.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toBadgeSet(data: String): Set<Badge> {
        return if (data.isEmpty()) {
            emptySet()
        } else {
            data.split(",").map { Badge.valueOf(it) }.toSet()
        }
    }

    @TypeConverter
    fun fromStringSet(strings: Set<String>): String {
        return strings.joinToString(",")
    }

    @TypeConverter
    fun toStringSet(data: String): Set<String> {
        return if (data.isEmpty()) {
            emptySet()
        } else {
            data.split(",").toSet()
        }
    }
}
