package com.example.mindarc.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Per-app usage entry for display. */
data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val usageMillis: Long
)

/** Today's screen time for social media apps (only those installed and with usage). */
data class SocialMediaScreenTime(
    val totalMillis: Long,
    val appUsages: List<AppUsageEntry>
)

/**
 * Usage tier for social media screen time (time slab).
 * Usage (Hrs/Day): Excellent 0-1, Good 1-2, Average 2-3, Below Average 3-4.5, Bad 4.5-6, Critical 6+
 */
enum class SocialMediaUsageTier(
    val label: String,
    val emoji: String
) {
    EXCELLENT("Excellent", "🏆"),           // 0 - 1 hr
    GOOD("Good", "👍"),                     // 1 - 2 hrs
    AVERAGE("Average", "😐"),               // 2 - 3 hrs
    BELOW_AVERAGE("Below Average", "📉"),   // 3 - 4.5 hrs
    BAD("Bad", "😟"),                       // 4.5 - 6 hrs
    CRITICAL("Critical", "🚨");             // 6+ hrs
}

/** Returns the usage tier for the given total daily social media time (in millis). */
fun getSocialMediaUsageTier(totalMillis: Long): SocialMediaUsageTier {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis)
    return when {
        minutes < 60 -> SocialMediaUsageTier.EXCELLENT       // 0 - 1 hr
        minutes < 120 -> SocialMediaUsageTier.GOOD          // 1 - 2 hrs
        minutes < 180 -> SocialMediaUsageTier.AVERAGE       // 2 - 3 hrs
        minutes < 270 -> SocialMediaUsageTier.BELOW_AVERAGE // 3 - 4.5 hrs (270 min)
        minutes < 360 -> SocialMediaUsageTier.BAD           // 4.5 - 6 hrs (360 min)
        else -> SocialMediaUsageTier.CRITICAL                // 6+ hrs
    }
}

/** Known social media app package names (prefix or full). Only installed ones are included. */
private val SOCIAL_MEDIA_PACKAGES = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "com.facebook.katana",
    "com.facebook.lite",
    "com.facebook.orca",
    "com.snapchat.android",
    "com.reddit.frontpage",
    "com.twitter.android",
    "com.x.android",
    "com.zhiliaoapp.musically",
    "com.tiktok",
    "com.linkedin.android",
    "org.telegram.messenger",
    "org.telegram.messenger.web",
    "com.discord",
    "com.pinterest",
    "com.google.android.youtube",
    "com.tencent.mm",
    "com.viber.voip",
    "com.skype.raider",
    "com.spotify.music"
)

class ScreenTimeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getTodayTotalScreenTime(): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        return stats?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    /** Returns today's combined screen time for social media apps that are installed. */
    fun getTodaySocialMediaScreenTime(): SocialMediaScreenTime {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ) ?: return SocialMediaScreenTime(0L, emptyList())

        val installedPackages = packageManager.getInstalledPackages(0).map { it.packageName }
        val socialPackageNames = installedPackages.filter { pkg ->
            SOCIAL_MEDIA_PACKAGES.any { pkg == it || pkg.startsWith("$it.") }
        }.toSet()

        val appUsages = mutableListOf<AppUsageEntry>()
        var totalMillis = 0L

        for (stat in stats) {
            if (stat.totalTimeInForeground <= 0) continue
            val pkg = stat.packageName
            if (pkg !in socialPackageNames) continue

            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: Exception) {
                pkg
            }
            appUsages.add(AppUsageEntry(packageName = pkg, appName = appName, usageMillis = stat.totalTimeInForeground))
            totalMillis += stat.totalTimeInForeground
        }

        appUsages.sortByDescending { it.usageMillis }
        return SocialMediaScreenTime(totalMillis = totalMillis, appUsages = appUsages)
    }

    fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            minutes > 0 -> String.format("%dm", minutes)
            else -> "0m"
        }
    }
}
