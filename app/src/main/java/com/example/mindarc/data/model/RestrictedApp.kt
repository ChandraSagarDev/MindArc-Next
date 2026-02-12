package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restricted_apps")
data class RestrictedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    var isBlocked: Boolean = true,
    val iconUri: String? = null,
    var dailyLimitInMillis: Long = 0,
    var usageTodayInMillis: Long = 0,
    var extraTimePurchased: Long = 0,
    var lastUsageTimestamp: Long = 0,
    var warningSent: Boolean = false
)

