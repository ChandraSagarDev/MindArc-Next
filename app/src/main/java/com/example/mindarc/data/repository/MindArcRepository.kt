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
    private val ioDispatcher: CoroutineDispatcher
) {
    private val packageManager: PackageManager = context.packageManager

    companion object {
        const val ACTION_RESTRICTIONS_UPDATED = "com.example.mindarc.service.blocking.RESTRICTIONS_UPDATED"
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

        return@withContext installedApps
            .mapNotNull { packageInfo ->
                packageInfo.applicationInfo
                    ?.takeIf { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    ?.let { appInfo ->
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val usageTime = usageStats?.find { it.packageName == appInfo.packageName }?.totalTimeInForeground ?: 0
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
    }

    // Rich overload for reading activities with badge/streak logic
    suspend fun updateProgressAfterActivity(
        activity: ActivityRecord,
        actualReadingTime: Int? = null
    ) = withContext(ioDispatcher) {
        val progress = getProgressSync() ?: UserProgress()
        var points = activity.pointsEarned
        var newPerfectScoreStreak = progress.perfectScoreStreak
        var newMultiplierEndTime = progress.multiplierEndTime

        if (activity.activityType == ActivityType.READING_APP_PROVIDED) {
            val isPerfectScore = activity.pointsEarned > 0 && activity.readingContentId != null &&
                quizQuestionDao.getQuestionsForContent(activity.readingContentId).all { it.correctAnswer == it.userAnswer }

            points = calculateReadingPoints(activity.unlockDurationMinutes, isPerfectScore)

            if (isPerfectScore) {
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
                if (isPerfectScore && activity.unlockDurationMinutes >= 12 && it < 15) {
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
    }

    suspend fun updateProgressAfterUnlock() = withContext(ioDispatcher) {
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        val updatedProgress = progress.copy(
            totalUnlockSessions = progress.totalUnlockSessions + 1
        )
        userProgressDao.updateProgress(updatedProgress)
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
                """.trimIndent(),
                estimatedReadingTimeMinutes = 4,
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
                """.trimIndent(),
                estimatedReadingTimeMinutes = 8,
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
            }
        }
    }
}
