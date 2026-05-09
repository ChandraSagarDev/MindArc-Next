package com.example.mindarc.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.mindarc.data.dao.ActivityRecordDao
import com.example.mindarc.data.dao.QuizQuestionDao
import com.example.mindarc.data.dao.ReadingContentDao
import com.example.mindarc.data.dao.ReadingReflectionDao
import com.example.mindarc.data.dao.RestrictedAppDao
import com.example.mindarc.data.dao.UnlockSessionDao
import com.example.mindarc.data.dao.UserProgressDao
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.Badge
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.model.ReadingContent
import com.example.mindarc.data.model.ReadingReflection
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.remote.FirebaseUserStore
import com.example.mindarc.data.remote.RemoteUserState
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MindArcRepository @Inject constructor(
    private val restrictedAppDao: RestrictedAppDao,
    private val activityRecordDao: ActivityRecordDao,
    private val unlockSessionDao: UnlockSessionDao,
    private val userProgressDao: UserProgressDao,
    private val readingContentDao: ReadingContentDao,
    private val quizQuestionDao: QuizQuestionDao,
    private val readingReflectionDao: ReadingReflectionDao,
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val firebaseUserStore: FirebaseUserStore,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val packageManager: PackageManager = context.packageManager

    companion object {
        const val ACTION_RESTRICTIONS_UPDATED = "com.example.mindarc.service.blocking.RESTRICTIONS_UPDATED"
        private const val KEY_COMMON_DAILY_LIMIT_MILLIS = "common_daily_limit_millis"
        private const val KEY_STEPS_REWARD_CLAIM_DAY = "steps_reward_claim_day"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ONBOARDING_GOAL = "onboarding_goal"
        private const val KEY_ONBOARDING_DAILY_TARGET_MINUTES = "onboarding_daily_target_minutes"
        private const val KEY_ONBOARDING_GOALS = "onboarding_goals"
        private const val KEY_ONBOARDING_DAILY_PHONE_HOURS = "onboarding_daily_phone_hours"
    }

    fun isOnboardingCompleted(): Boolean = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    suspend fun setOnboardingCompleted(completed: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)?.trim()?.takeIf { it.isNotBlank() }

    suspend fun setUserName(name: String) = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    suspend fun setOnboardingGoal(goal: String) = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_ONBOARDING_GOAL, goal).apply()
    }

    fun getOnboardingGoal(): String? = sharedPreferences.getString(KEY_ONBOARDING_GOAL, null)?.trim()?.takeIf { it.isNotBlank() }

    suspend fun setOnboardingGoals(goals: List<String>) = withContext(ioDispatcher) {
        val sanitized = goals.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(3)
        sharedPreferences.edit().putString(KEY_ONBOARDING_GOALS, sanitized.joinToString("|")).apply()
    }

    fun getOnboardingGoals(): List<String> {
        val raw = sharedPreferences.getString(KEY_ONBOARDING_GOALS, null) ?: return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct().take(3)
    }

    suspend fun setOnboardingDailyTargetMinutes(minutes: Int) = withContext(ioDispatcher) {
        sharedPreferences.edit().putInt(KEY_ONBOARDING_DAILY_TARGET_MINUTES, minutes).apply()
    }

    fun getOnboardingDailyTargetMinutes(): Int = sharedPreferences.getInt(KEY_ONBOARDING_DAILY_TARGET_MINUTES, 30)

    suspend fun setOnboardingDailyPhoneHours(hours: Int) = withContext(ioDispatcher) {
        sharedPreferences.edit().putInt(KEY_ONBOARDING_DAILY_PHONE_HOURS, hours.coerceIn(0, 24)).apply()
    }

    fun getOnboardingDailyPhoneHours(): Int = sharedPreferences.getInt(KEY_ONBOARDING_DAILY_PHONE_HOURS, 4)

    /** Common daily time limit (millis) for all blocked apps combined. 0 = no limit. */
    fun getCommonDailyLimitMillis(): Long = sharedPreferences.getLong(KEY_COMMON_DAILY_LIMIT_MILLIS, 0L)

    suspend fun setCommonDailyLimitMillis(millis: Long) = withContext(ioDispatcher) {
        sharedPreferences.edit().putLong(KEY_COMMON_DAILY_LIMIT_MILLIS, millis).apply()
    }

    // Restricted Apps
    fun getAllApps(): Flow<List<RestrictedApp>> = restrictedAppDao.getAllApps()
    fun getBlockedApps(): Flow<List<RestrictedApp>> = restrictedAppDao.getBlockedApps()
    suspend fun getAppByPackageName(packageName: String): RestrictedApp? = withContext(ioDispatcher) { restrictedAppDao.getAppByPackageName(packageName) }
    suspend fun insertApp(app: RestrictedApp) = withContext(ioDispatcher) {
        restrictedAppDao.insertApp(app)
        notifyService()
    }
    suspend fun updateApp(app: RestrictedApp) = withContext(ioDispatcher) {
        restrictedAppDao.updateApp(app)
        notifyService()
    }
    suspend fun deleteApp(packageName: String) = withContext(ioDispatcher) {
        restrictedAppDao.deleteAppByPackageName(packageName)
        notifyService()
    }
    suspend fun updateUsage(packageName: String, usage: Long) = withContext(ioDispatcher) {
        val app = restrictedAppDao.getAppByPackageName(packageName)
        app?.let {
            it.usageTodayInMillis += usage
            restrictedAppDao.updateApp(it)
        }
    }

    private fun notifyService() {
        val intent = Intent(ACTION_RESTRICTIONS_UPDATED)
        context.sendBroadcast(intent)
    }

    suspend fun resetDailyStats() = withContext(ioDispatcher) {
        val apps = restrictedAppDao.getAllApps().first()
        apps.forEach { app ->
            if (app.usageTodayInMillis > 0 || app.warningSent || app.extraTimePurchased > 0) {
                val updatedApp = app.copy(
                    usageTodayInMillis = 0,
                    extraTimePurchased = 0,
                    warningSent = false
                )
                restrictedAppDao.updateApp(updatedApp)
            }
        }
    }

    suspend fun getInstalledApps(): List<RestrictedApp> = withContext(ioDispatcher) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val installedApps = packageManager.getInstalledPackages(0)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val usageByPackage = usageStats?.associateBy({ it.packageName }, { it.totalTimeInForeground }) ?: emptyMap()

        return@withContext installedApps
            .mapNotNull { packageInfo ->
                packageInfo.applicationInfo
                    ?.takeIf { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    ?.let { appInfo ->
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val usageTime = usageByPackage[appInfo.packageName] ?: 0L
                        RestrictedApp(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            isBlocked = false,
                            usageTodayInMillis = usageTime
                        )
                    }
            }
    }

    // Activities
    fun getAllActivities(): Flow<List<ActivityRecord>> = activityRecordDao.getAllActivities()
    suspend fun insertActivity(activity: ActivityRecord): Long = withContext(ioDispatcher) { activityRecordDao.insertActivity(activity) }

    fun calculateUnlockDuration(pushups: Int): Int {
        return (pushups * 15) / 10
    }

    fun calculatePoints(pushups: Int): Int {
        return pushups
    }

    fun calculatePlankPoints(secondsHeld: Int): Int {
        // 1 point per 10 seconds, min 1
        return (secondsHeld / 10).coerceAtLeast(1)
    }

    fun calculatePlankUnlockDurationMinutes(secondsHeld: Int): Int {
        // ~1 minute per 15 seconds, min 1, cap at 60
        return (secondsHeld / 15).coerceAtLeast(1).coerceAtMost(60)
    }

    fun calculateStepsPoints(steps: Long): Int {
        // 1 point per 100 steps, min 1, cap at 200
        return (steps / 100L).toInt().coerceAtLeast(1).coerceAtMost(200)
    }

    fun calculateStepsUnlockDurationMinutes(steps: Long): Int {
        // 1 minute per 200 steps, min 1, cap at 120
        return (steps / 200L).toInt().coerceAtLeast(1).coerceAtMost(120)
    }

    fun canClaimStepsRewardToday(): Boolean {
        val calendar = Calendar.getInstance()
        val todayKey = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        val lastClaimDay = sharedPreferences.getInt(KEY_STEPS_REWARD_CLAIM_DAY, -1)
        return lastClaimDay != todayKey
    }

    suspend fun markStepsRewardClaimedToday() = withContext(ioDispatcher) {
        val calendar = Calendar.getInstance()
        val todayKey = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        sharedPreferences.edit().putInt(KEY_STEPS_REWARD_CLAIM_DAY, todayKey).apply()
    }

    fun calculateReadingUnlockDuration(minutes: Int, isPerfectScore: Boolean = false): Int {
        return if (isPerfectScore) {
            (minutes * 1.25).toInt()
        } else {
            minutes
        }
    }

    suspend fun calculateReadingPoints(minutes: Int, isPerfectScore: Boolean = false): Int {
        var basePoints = minutes * 2
        if (isPerfectScore) {
            basePoints *= 2
        }
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        if (System.currentTimeMillis() < progress.multiplierEndTime) {
            basePoints = (basePoints * 1.5).toInt()
        }
        return basePoints
    }

    fun getUserLevelTitle(points: Int): String {
        return when {
            points >= 3000 -> "Zen Grandmaster"
            points >= 1500 -> "Mindfulness Master"
            points >= 500 -> "Scholar"
            points >= 100 -> "Apprentice"
            else -> "Novice"
        }
    }

    // Reading Reflections
    suspend fun insertReflection(reflection: ReadingReflection) = withContext(ioDispatcher) {
        readingReflectionDao.insertReflection(reflection)
    }

    // Unlock Sessions
    suspend fun getActiveSession(): UnlockSession? = withContext(ioDispatcher) { unlockSessionDao.getActiveSession() }
    fun getAllSessions(): Flow<List<UnlockSession>> = unlockSessionDao.getAllSessions()

    suspend fun createUnlockSession(activityRecordId: Long, durationMinutes: Int): UnlockSession = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMinutes * 60 * 1000L)
        unlockSessionDao.deactivateAllSessions()
        val session = UnlockSession(
            activityRecordId = activityRecordId,
            startTime = startTime,
            endTime = endTime,
            isActive = true
        )
        unlockSessionDao.insertSession(session)
        return@withContext session
    }

    suspend fun checkAndDeactivateExpiredSessions() = withContext(ioDispatcher) {
        val activeSession = unlockSessionDao.getActiveSession()
        activeSession?.let {
            if (System.currentTimeMillis() >= it.endTime) {
                unlockSessionDao.deactivateAllSessions()
            }
        }
    }

    // User Progress
    fun getProgress(): Flow<UserProgress?> = userProgressDao.getProgress()
    suspend fun getProgressSync(): UserProgress? = withContext(ioDispatcher) { userProgressDao.getProgressSync() }

    suspend fun updateProgress(progress: UserProgress) = withContext(ioDispatcher) {
        userProgressDao.updateProgress(progress)
    }

    suspend fun ensureRemoteUserDoc() {
        try {
            firebaseUserStore.ensureUserDocExists()
        } catch (e: Exception) {
            Log.w("MindArcRepository", "Failed to ensure remote user doc", e)
        }
    }

    suspend fun syncLocalTotalPointsToRemote(totalPoints: Int) = withContext(ioDispatcher) {
        try {
            firebaseUserStore.ensureUserDocExists()
            firebaseUserStore.setRemoteTotalPoints(totalPoints.toLong())
        } catch (e: Exception) {
            Log.w("MindArcRepository", "Failed to sync points to remote", e)
        }
    }

    suspend fun incrementRemotePoints(points: Int) = withContext(ioDispatcher) {
        if (points == 0) return@withContext
        try {
            firebaseUserStore.ensureUserDocExists()
            firebaseUserStore.incrementPoints(points)
        } catch (e: Exception) {
            Log.w("MindArcRepository", "Failed to increment remote points", e)
        }
    }

    suspend fun spendRemotePointsAndSetUnlock(points: Int, durationMinutes: Int): Boolean = withContext(ioDispatcher) {
        try {
            firebaseUserStore.ensureUserDocExists()
            firebaseUserStore.spendPointsAndSetUnlock(points, durationMinutes)
        } catch (e: Exception) {
            Log.w("MindArcRepository", "Failed to spend remote points", e)
            false
        }
    }

    suspend fun getRemoteUserState(): RemoteUserState? = withContext(ioDispatcher) {
        try {
            firebaseUserStore.ensureUserDocExists()
            firebaseUserStore.getRemoteState()
        } catch (e: Exception) {
            Log.w("MindArcRepository", "Failed to read remote user state", e)
            null
        }
    }

    // Simple overload for exercise activities (pushups, squats)
    suspend fun updateProgressAfterActivity(points: Int) = withContext(ioDispatcher) {
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val lastActivityDate = progress.lastActivityDate
        val newStreak = if (lastActivityDate != null) {
            val daysDiff = (today - lastActivityDate) / (1000 * 60 * 60 * 24)
            when {
                daysDiff == 0L -> progress.currentStreak
                daysDiff == 1L -> progress.currentStreak + 1
                else -> 1
            }
        } else {
            1
        }

        val updatedProgress = progress.copy(
            totalPoints = progress.totalPoints + points,
            currentStreak = newStreak,
            longestStreak = maxOf(progress.longestStreak, newStreak),
            lastActivityDate = today,
            totalActivities = progress.totalActivities + 1
        )
        userProgressDao.updateProgress(updatedProgress)
        if (points != 0) {
            try {
                firebaseUserStore.ensureUserDocExists()
                firebaseUserStore.incrementPoints(points)
            } catch (e: Exception) {
                Log.w("MindArcRepository", "Failed to sync points after activity", e)
            }
        }
    }

    // Rich overload for reading activities with badge/streak logic
    suspend fun updateProgressAfterActivity(
        activity: ActivityRecord,
        actualReadingTime: Int? = null,
        appProvidedLeftApp: Boolean = false,
        appProvidedQuizPerfect: Boolean = false
    ) = withContext(ioDispatcher) {
        val progress = getProgressSync() ?: UserProgress()
        var points = activity.pointsEarned
        var newPerfectScoreStreak = progress.perfectScoreStreak
        var newMultiplierEndTime = progress.multiplierEndTime

        if (activity.activityType == ActivityType.READING_APP_PROVIDED) {
            val effectivePerfectScore = appProvidedQuizPerfect && !appProvidedLeftApp

            if (effectivePerfectScore) {
                newPerfectScoreStreak++
                if (newPerfectScoreStreak >= 3) {
                    newMultiplierEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
                    Log.i("MindArcProgress", "SCHOLAR'S STREAK! 1.5x points for 24 hours.")
                }
            } else {
                newPerfectScoreStreak = 0
            }

            if (progress.totalActivities == 0) {
                userProgressDao.addBadge(Badge.FIRST_EDITION)
                Log.i("MindArcProgress", "BADGE EARNED: First Edition")
            }

            actualReadingTime?.let {
                if (effectivePerfectScore && activity.unlockDurationMinutes >= 12 && it < 15) {
                    userProgressDao.addBadge(Badge.SPEED_READER)
                    Log.i("MindArcProgress", "BADGE EARNED: Speed Reader")
                }
            }

            val uniqueCategories = activityRecordDao.getUniqueCategoriesCompleted()
            if (uniqueCategories.size >= 3) {
                userProgressDao.addBadge(Badge.POLYMATH)
                Log.i("MindArcProgress", "BADGE EARNED: Polymath")
            }
        }

        val oldTotalPoints = progress.totalPoints
        val newTotalPoints = oldTotalPoints + points

        val oldLevel = getUserLevelTitle(oldTotalPoints)
        val newLevel = getUserLevelTitle(newTotalPoints)

        if (oldLevel != newLevel) {
            Log.i("MindArcProgress", "LEVEL UP! User advanced from $oldLevel to $newLevel")
        }

        val updatedProgress = progress.copy(
            totalPoints = newTotalPoints,
            currentStreak = if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != progress.lastActivityDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_YEAR) }) progress.currentStreak + 1 else progress.currentStreak,
            longestStreak = maxOf(progress.longestStreak, if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != progress.lastActivityDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_YEAR) }) progress.currentStreak + 1 else progress.currentStreak),
            lastActivityDate = System.currentTimeMillis(),
            totalActivities = progress.totalActivities + 1,
            perfectScoreStreak = newPerfectScoreStreak,
            multiplierEndTime = newMultiplierEndTime
        )
        updateProgress(updatedProgress)
        if (points != 0) {
            try {
                firebaseUserStore.ensureUserDocExists()
                firebaseUserStore.incrementPoints(points)
            } catch (e: Exception) {
                Log.w("MindArcRepository", "Failed to sync points after activity", e)
            }
        }
    }

    suspend fun updateProgressAfterUnlock() = withContext(ioDispatcher) {
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        val updatedProgress = progress.copy(
            totalUnlockSessions = progress.totalUnlockSessions + 1
        )
        userProgressDao.updateProgress(updatedProgress)
    }

    // Badges
    suspend fun awardBadge(badge: Badge) = withContext(ioDispatcher) {
        userProgressDao.addBadge(badge)
    }

    // Reading Content
    suspend fun getRandomReadingContent(): ReadingContent? = withContext(ioDispatcher) { readingContentDao.getRandomContent() }
    suspend fun getReadingContentById(id: Long): ReadingContent? = withContext(ioDispatcher) { readingContentDao.getContentById(id) }
    suspend fun insertReadingContent(content: ReadingContent): Long = withContext(ioDispatcher) { readingContentDao.insertContent(content) }

    // Quiz Questions
    suspend fun getQuestionsForContent(contentId: Long, limit: Int = 3): List<QuizQuestion> = withContext(ioDispatcher) {
        return@withContext quizQuestionDao.getRandomQuestionsForContent(contentId, limit)
    }

    suspend fun insertQuizQuestions(questions: List<QuizQuestion>) = withContext(ioDispatcher) {
        quizQuestionDao.insertQuestions(questions)
    }

    // Initialize default data
    suspend fun initializeDefaultData() = withContext(ioDispatcher) {
        val progress = userProgressDao.getProgressSync()
        if (progress == null) {
            userProgressDao.insertProgress(UserProgress())
        }
        val contentCount = readingContentDao.getAllContent().first().size
        if (contentCount == 0) {
            initializeDefaultReadingContent()
        }
    }

    private suspend fun initializeDefaultReadingContent() {
        val contents = listOf(
            ReadingContent(
                title = "The Power of Small Habits",
                content = """
                    Small habits can have a profound impact on our lives. When we commit to doing something small every day, 
                    we create a compound effect that leads to significant change over time. The key is consistency. 
                    It's not about doing something perfectly or for a long time. It's about showing up every day, 
                    even if it's just for a few minutes.
                    
                    Research shows that it takes an average of 66 days to form a new habit. But the journey is more 
                    important than the destination. Each day you practice, you're strengthening the neural pathways 
                    that make the habit easier to maintain. This biological reinforcement is what eventually turns 
                    an effortful action into an automatic response.
                    
                    Atomic habits, as described by experts, suggest that improvements of just 1% each day lead to 
                    nearly 37 times better results after one year. This isn't just motivational talk; it's a 
                    mathematical reality of compound growth. When we break down monumental goals into manageable 
                    daily actions, we bypass the brain's natural resistance to change, known as homeostasis.
                    
                    Furthermore, environment plays a crucial role in habit formation. If you want to make a habit 
                    easier, you should design your environment to encourage it. Keep your book on your pillow, 
                    your running shoes by the door, and your distractions out of reach. By reducing friction, 
                    you increase the likelihood of actually performing the habit without relying solely on willpower.
                    
                    The concept of "habit stacking" is another powerful tool. By anchoring a new habit to an 
                    existing one—like doing five pushups immediately after brushing your teeth—you leverage the 
                    established neural networks in your brain. This creates a natural trigger that reminds 
                    you to act without needing a conscious prompt or external reminder.
                    
                    Finally, identity-based habits are the most sustainable. Instead of focusing on what you 
                    want to achieve, focus on who you want to become. Don't just try to read a book; decide 
                    to become a reader. When your actions align with your identity, they no longer feel 
                    like chores, but like natural expressions of who you are.
                    
                    Remember, every expert was once a beginner. Every pro was once an amateur. The difference 
                    is they kept going when others gave up. Start small, stay consistent, and trust the process. 
                    The size of the initial step doesn't matter as much as the direction and the persistence.

                    A practical way to start is to define your "minimum viable habit." Ask: what is the smallest
                    version of this behavior that I can do even on my worst day? If the answer is too large,
                    shrink it. Your goal is not to feel motivated; it's to make the behavior easy to begin.
                    Once the habit is stable, you can slowly increase the difficulty.

                    Another useful strategy is to make the cue impossible to miss and the action impossible
                    to ignore. Put your tools in the places you will naturally reach for them. Then remove
                    the need for decisions: pre-pack the bag, pre-open the document, and set a predictable time
                    window. Habit formation becomes simpler when the brain does not have to negotiate.

                    Finally, measure progress in a way that keeps you moving. Track the days you completed the habit,
                    not the size of your output. When you focus on consistency, your results tend to follow.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 6,
                category = "Self-Improvement"
            ),
            ReadingContent(
                title = "Digital Wellness in the Modern Age",
                content = """
                    In today's hyperconnected world, managing our relationship with technology has become more 
                    important than ever. Digital wellness isn't about completely avoiding technology, but about 
                    using it mindfully and intentionally. It's about ensuring that our tools serve us, rather 
                    than the other way around.
                    
                    The average person checks their phone over 150 times a day. Many of these checks are 
                    automatic, driven by habit rather than necessity. This constant connectivity can lead 
                    to increased stress, decreased focus, and reduced quality of sleep. Our brains were not 
                    evolved to handle the infinite scroll and the dopamine loops of modern social media.
                    
                    To deepen our understanding, we should consider the concept of "Digital Minimalism." 
                    This philosophy suggests that you should focus your online time on a small number of 
                    carefully selected and optimized activities that strongly support things you value. 
                    It's not about missing out; it's about choosing what is worth your limited attention.
                    
                    Setting boundaries is a crucial skill for digital wellness. This includes designating 
                    tech-free zones, such as the dining table or the bedroom, and taking regular "digital 
                    detoxes" to reset your cognitive baseline. Being present in the moment allows us to 
                    reconnect with our physical surroundings and the people around us without digital interference.
                    
                    The impact of blue light on our circadian rhythm is well-documented. Digital wellness 
                    also involves physical health. Limiting screen exposure at least an hour before bed 
                    can significantly improve sleep quality. Better sleep leads to improved emotional 
                    regulation and higher cognitive performance the next day, creating a positive cycle.
                    
                    One effective strategy is to link screen time to productive activities. By earning 
                    your screen time through exercise or reading, you create a positive feedback loop. 
                    This transforms passive consumption into a reward for active contribution, 
                    helping to rewire your brain's association with digital entertainment.
                    
                    We must also be aware of the "Attention Economy." Platforms are designed to keep us 
                    engaged for as long as possible because our attention is their primary product. 
                    By understanding these psychological hooks, we can build defenses against them, 
                    such as disabling non-essential notifications and choosing tools that respect our focus.
                    
                    In conclusion, digital wellness is an ongoing practice of self-awareness. It requires 
                    us to regularly evaluate our digital habits and make adjustments. Technology is a 
                    magnificent tool, but like any tool, it's most effective when used with intention 
                    and purpose, not as a default escape from boredom or discomfort.

                    A helpful next step is to replace what you remove. When you delete a distracting app or turn off
                    a category of notifications, create a better default behavior that you can return to. For example,
                    if you reduce late-night scrolling, consider a short reading routine, a calming playlist, or a quick
                    review of what you want to do tomorrow.

                    You can also make your limits visible. Think of your screen time like a budget: set a cap, display
                    the progress, and let yourself make small trades that align with your values. Rather than relying on
                    willpower, design the system so it nudges you toward the choices you want to repeat.

                    Over time, your relationship with technology becomes less reactive and more deliberate.
                    The objective is not perfect restraint; it's a steadier attention rhythm that supports sleep,
                    focus, and real-world connection.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 10,
                category = "Wellness"
            ),
            ReadingContent(
                title = "The Science of Focus",
                content = """
                    Focus is not just about willpower—it's a skill that can be developed and strengthened 
                    over time. Understanding how focus works can help us improve our ability to concentrate 
                    on what matters. In an age of distraction, the ability to focus is becoming a rare 
                    and valuable commodity in the professional landscape.
                    
                    The brain has two main modes: focused mode and diffuse mode. Focused mode is when 
                    we're actively concentrating on a task, utilizing the prefrontal cortex. Diffuse mode 
                    is when our mind wanders, allowing the "default mode network" to make creative 
                    connections between seemingly unrelated ideas. Both modes are essential for learning.
                    
                    However, constant distractions can prevent us from entering either mode effectively. 
                    When we're constantly switching between tasks—a phenomenon known as context switching—we 
                    incur a "switching cost" that reduces our IQ and productivity. We never give our 
                    brain a chance to fully engage with the complexity of a single problem.
                    
                    Exploring deeper, "Deep Work" is the ability to focus without distraction on a 
                    cognitively demanding task. It's a superpower in our increasingly fragmented 
                    economy. Those who can cultivate this skill will thrive, as shallow work—like 
                    answering emails or attending unnecessary meetings—is easily replaceable and adds less value.
                    
                    To improve focus, we need to create an environment that minimizes distractions. 
                    This might mean turning off all notifications, setting specific blocks of time 
                    for focused work, and using techniques like the Pomodoro method to maintain 
                    intensity. A clear physical workspace often leads to a clearer mental workspace.
                    
                    Another critical factor is our physiological state. Hydration, nutrition, and 
                    movement all influence our cognitive capacity. If you're struggling to focus, 
                    sometimes the best solution isn't more discipline, but a short walk or a glass 
                    of water to reset your brain's chemistry and improve blood flow to the brain.
                    
                    The role of dopamine in focus is often misunderstood. While dopamine is linked 
                    to reward, it is also crucial for motivation and attention. Engaging in 
                    high-dopamine activities like checking social media can deplete our "focus 
                    reserves," making it harder to concentrate on less stimulating but more 
                    important tasks later in the day.
                    
                    Mindfulness and meditation are scientifically proven to strengthen the neural 
                    pathways associated with attention. Even five minutes of daily practice can 
                    increase the density of gray matter in regions of the brain responsible for 
                    executive function, helping us notice when our mind has wandered and 
                    gently bringing it back.
                    
                    The concept of "Flow," described by psychologists as being "in the zone," is 
                    the ultimate state of focus. It occurs when the challenge of a task perfectly 
                    matches our skill level. In this state, self-consciousness vanishes, and time 
                    seems to disappear. Designing our work to reach this state is the key to peak performance.
                    
                    In summary, focus is like a muscle that requires both exercise and recovery. 
                    We cannot expect to be focused 24/7. By respecting our biological limits, 
                    optimizing our environment, and practicing concentration, we can reclaim 
                    our most valuable resource: our attention. It is through focus that we 
                    achieve our greatest potential and find deepest satisfaction in our work.

                    A simple focus protocol can start with three phases. First, prepare: silence notifications,
                    close extra tabs, and define the smallest "done" for the next session. Second, protect your
                    attention: stay with the task and notice when your mind drifts. When it drifts, gently return
                    without punishment. Third, recover: end the session intentionally so your brain can relax and reset.

                    Recovery is not a reward you earn at the end. It is part of the training. Even short breaks help
                    your nervous system clear the mental noise that builds up during deep work. A walk, stretching,
                    hydration, or a few minutes of calm breathing can be enough to restore clarity.

                    If you want to go further, practice "friction removal" for your focus tools. Use a single place
                    to capture tasks, keep a dedicated workspace, and make it easy to start. Focus improves when
                    starting is the hard part, not continuing.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 14,
                category = "Productivity"
            ),
            // Additional curated articles
            ReadingContent(
                title = "The Habit Loop: Cue, Action, Reward",
                content = """
                    Habits are not random. Most of what we call “instinct” is actually a repeating loop in the brain.
                    A cue appears first, then an action follows, and finally the reward seals the pattern.
                    
                    When you want a healthier phone routine, start by noticing your cues. What happens right before you open
                    a distracting app? Boredom, stress, loneliness, or simply a sudden chunk of free time? Write it down
                    for a few days. Labels matter because they make the pattern visible.
                    
                    Next, choose an alternative action that delivers a similar reward. If the reward is calm or comfort,
                    try a short reading session, a breathing timer, or a quick “reset” checklist rather than endless scrolling.
                    The point is not to remove the reward; it is to change how you earn it.
                    
                    Finally, make the cue easier to respond to and the old action harder to do. You can do this with friction:
                    move the app away from the home screen, disable the strongest notification types, or require a short
                    permission/break step. When friction increases, new loops can finally get traction.
                    
                    Over time, your brain will learn the updated sequence. The loop becomes your ally when you repeat it consistently.
                    A small habit, repeated on purpose, is how you rewrite the day.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 10,
                category = "Behavior Change"
            ),
            ReadingContent(
                title = "Designing Notifications for Focus",
                content = """
                    Notifications are powerful because they interrupt. Even when they are pleasant, the interruption fragments
                    attention and forces your brain to context-switch back into the task.
                    
                    A strong approach is to treat notifications like a diet. You do not need “less information,” you need a
                    better selection of what arrives. Decide which notifications truly deserve real-time attention.
                    
                    Start with tiers. Tier 1 might include calls and messages from a small set of important people.
                    Tier 2 includes work reminders that can wait until a specific time window. Tier 3 includes everything else.
                    
                    Then align each tier with a behavior. Tier 1 is allowed, but only when you are available to respond.
                    Tier 2 becomes a scheduled batch: review it twice per day.
                    Tier 3 stays quiet, with notifications disabled or collected for later.
                    
                    This is not about self-control alone. It is about choosing defaults that protect your attention.
                    When you reduce interruptions, you create more space for deep work and for the kinds of reading that make
                    learning stick.
                    
                    If you want an easy win, pick one app today and change only one setting: either silence it completely
                    or restrict it to scheduled checks. Consistency beats complexity.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 9,
                category = "Digital Wellness"
            ),
            ReadingContent(
                title = "Deep Work: Scheduling the Right Kind of Time",
                content = """
                    Deep work is not just time you put on a calendar. It is a structure that makes distraction less likely.
                    When you schedule deep work, you are making a promise to your future attention.
                    
                    Begin by picking a focus outcome, not a vague goal. Instead of “work on the project,” choose a specific deliverable,
                    like “finish the outline” or “draft three sections.” A clear outcome makes it easier to know when the session is done.
                    
                    Next, set a time boundary. Choose a session length you can protect without resentment.
                    For many people, 25 to 45 minutes is a good start. If you are new to focus, shorter sessions build trust.
                    
                    Then remove friction. Close extra apps, put your phone out of reach, and prepare the next input before the session begins.
                    A surprising amount of “starting delay” comes from minor setup choices.
                    
                    Finally, practice a clean ending. When your session ends, write a quick “next step” note.
                    This reduces mental load and makes the following session easier to start.
                    
                    Deep work becomes a habit the same way any habit does: with cues, actions, and rewards.
                    The reward can be as simple as a satisfied checkmark and the feeling of having truly completed something.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 12,
                category = "Productivity"
            )
        )

        contents.forEach { content ->
            val contentId = readingContentDao.insertContent(content)
            
            when (content.title) {
                "The Power of Small Habits" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "How long does it take on average to form a new habit?", correctAnswer = "66 days", option1 = "21 days", option2 = "66 days", option3 = "90 days", option4 = "100 days"),
                        QuizQuestion(readingContentId = contentId, question = "What is the improvement percentage per day that leads to 37x results in a year?", correctAnswer = "1%", option1 = "1%", option2 = "5%", option3 = "10%", option4 = "0.5%"),
                        QuizQuestion(readingContentId = contentId, question = "What is the term for anchoring a new habit to an existing one?", correctAnswer = "Habit stacking", option1 = "Habit pairing", option2 = "Habit stacking", option3 = "Habit linking", option4 = "Habit grouping"),
                        QuizQuestion(readingContentId = contentId, question = "According to the text, which type of habits are most sustainable?", correctAnswer = "Identity-based habits", option1 = "Goal-based habits", option2 = "Identity-based habits", option3 = "Reward-based habits", option4 = "Time-based habits"),
                        QuizQuestion(readingContentId = contentId, question = "What is the brain's natural resistance to change called?", correctAnswer = "Homeostasis", option1 = "Inertia", option2 = "Stagnation", option3 = "Homeostasis", option4 = "Resistance")
                    ))
                }
                "Digital Wellness in the Modern Age" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "How many times does the average person check their phone per day?", correctAnswer = "Over 150 times", option1 = "50 times", option2 = "100 times", option3 = "Over 150 times", option4 = "200 times"),
                        QuizQuestion(readingContentId = contentId, question = "What philosophy suggests focusing online time on activities that support your values?", correctAnswer = "Digital Minimalism", option1 = "Digital Minimalism", option2 = "Digital Essentialism", option3 = "Digital Abstinence", option4 = "Digital Intentionality"),
                        QuizQuestion(readingContentId = contentId, question = "What should you limit at least an hour before bed to improve sleep quality?", correctAnswer = "Blue light exposure", option1 = "Caffeine", option2 = "Blue light exposure", option3 = "Sugar", option4 = "Exercise"),
                        QuizQuestion(readingContentId = contentId, question = "What is the primary product of platforms in the 'Attention Economy'?", correctAnswer = "Our attention", option1 = "Our data", option2 = "Software", option3 = "Our attention", option4 = "Ads"),
                        QuizQuestion(readingContentId = contentId, question = "What is the recommended approach to technology use?", correctAnswer = "Using it mindfully and intentionally", option1 = "Avoiding it completely", option2 = "Using it mindfully and intentionally", option3 = "Using it as much as possible", option4 = "Only using it for work")
                    ))
                }
                "The Science of Focus" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "What is the brain's 'default mode network' associated with?", correctAnswer = "Diffuse mode", option1 = "Focused mode", option2 = "Diffuse mode", option3 = "Sleep mode", option4 = "Active mode"),
                        QuizQuestion(readingContentId = contentId, question = "What is the negative effect of constantly switching between tasks?", correctAnswer = "Switching cost", option1 = "Switching cost", option2 = "Focus fatigue", option3 = "Mental drain", option4 = "Context loss"),
                        QuizQuestion(readingContentId = contentId, question = "What neurochemical is linked to both reward and attention?", correctAnswer = "Dopamine", option1 = "Serotonin", option2 = "Dopamine", option3 = "Cortisol", option4 = "Melatonin"),
                        QuizQuestion(readingContentId = contentId, question = "What is the ultimate state of focus where self-consciousness vanishes?", correctAnswer = "Flow", option1 = "Zen", option2 = "Clarity", option3 = "Flow", option4 = "Trance"),
                        QuizQuestion(readingContentId = contentId, question = "Which brain region is primarily used during focused mode?", correctAnswer = "Prefrontal cortex", option1 = "Amygdala", option2 = "Prefrontal cortex", option3 = "Cerebellum", option4 = "Occipital lobe")
                    ))
                }
                "The Habit Loop: Cue, Action, Reward" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "What is the first component in the habit loop?", correctAnswer = "Cue", option1 = "Action", option2 = "Cue", option3 = "Reward", option4 = "Routine"),
                        QuizQuestion(readingContentId = contentId, question = "What should you write down to make phone patterns visible?", correctAnswer = "The cue before the action", option1 = "The app name", option2 = "The cue before the action", option3 = "The device battery level", option4 = "Your mood score"),
                        QuizQuestion(readingContentId = contentId, question = "What is the main idea of changing an old action?", correctAnswer = "Keep the reward, change the method", option1 = "Remove the reward completely", option2 = "Keep the reward, change the method", option3 = "Only change the schedule", option4 = "Increase screen time temporarily"),
                        QuizQuestion(readingContentId = contentId, question = "What helps new loops gain traction?", correctAnswer = "Consistency with friction changes", option1 = "Inconsistent effort", option2 = "Consistency with friction changes", option3 = "Multitasking", option4 = "Avoiding all habits"),
                        QuizQuestion(readingContentId = contentId, question = "What does a small habit repeated on purpose do?", correctAnswer = "Rewrites your day", option1 = "Replaces all skills", option2 = "Rewrites your day", option3 = "Eliminates cues", option4 = "Cancels rewards")
                    ))
                }
                "Designing Notifications for Focus" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "Why do notifications disrupt attention?", correctAnswer = "They interrupt and cause context switching", option1 = "They interrupt and cause context switching", option2 = "They increase deep work automatically", option3 = "They make tasks finish faster by default", option4 = "They remove mental load instantly"),
                        QuizQuestion(readingContentId = contentId, question = "What is the diet analogy used for notifications?", correctAnswer = "Better selection of what arrives", option1 = "More notifications for motivation", option2 = "Better selection of what arrives", option3 = "Avoiding all information forever", option4 = "Choosing random timing"),
                        QuizQuestion(readingContentId = contentId, question = "Which tier is allowed only when you are available to respond?", correctAnswer = "Tier 1", option1 = "Tier 1", option2 = "Tier 2", option3 = "Tier 3", option4 = "Tier 4"),
                        QuizQuestion(readingContentId = contentId, question = "What does Tier 2 become?", correctAnswer = "A scheduled batch review", option1 = "Real-time alerts", option2 = "A scheduled batch review", option3 = "Permanent silence only", option4 = "Random bursts throughout the day"),
                        QuizQuestion(readingContentId = contentId, question = "What is a suggested easy win?", correctAnswer = "Change one setting for one app", option1 = "Switch every app at once", option2 = "Change one setting for one app", option3 = "Increase all notification volume", option4 = "Turn everything back on after one day")
                    ))
                }
                "Deep Work: Scheduling the Right Kind of Time" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(readingContentId = contentId, question = "Deep work scheduling protects what?", correctAnswer = "Your attention", option1 = "Your screen brightness", option2 = "Your attention", option3 = "Your storage capacity", option4 = "Your battery percentage"),
                        QuizQuestion(readingContentId = contentId, question = "A focus outcome should be defined as a:", correctAnswer = "Specific deliverable", option1 = "Vague goal", option2 = "Specific deliverable", option3 = "Long vacation plan", option4 = "A random search result"),
                        QuizQuestion(readingContentId = contentId, question = "What is a good starting session length range?", correctAnswer = "25 to 45 minutes", option1 = "5 to 10 minutes", option2 = "25 to 45 minutes", option3 = "2 to 3 hours", option4 = "All day continuously"),
                        QuizQuestion(readingContentId = contentId, question = "What reduces starting delay?", correctAnswer = "Removing friction and preparing the next input", option1 = "Multitasking", option2 = "Removing friction and preparing the next input", option3 = "Keeping your phone on you", option4 = "Skipping setup before sessions"),
                        QuizQuestion(readingContentId = contentId, question = "What should you write at the end of a session?", correctAnswer = "A quick next step note", option1 = "A full autobiography", option2 = "A quick next step note", option3 = "Your password", option4 = "A random quote")
                    ))
                }
            }
        }
    }
}
