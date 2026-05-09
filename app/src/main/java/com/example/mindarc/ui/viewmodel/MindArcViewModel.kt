package com.example.mindarc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.domain.SocialMediaScreenTime
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.domain.ScreenTimeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MindArcViewModel @Inject constructor(
    private val repository: MindArcRepository,
    private val screenTimeManager: ScreenTimeManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _restrictedApps = MutableStateFlow<List<RestrictedApp>>(emptyList())
    val restrictedApps: StateFlow<List<RestrictedApp>> = _restrictedApps.asStateFlow()

    private val _displayedApps = MutableStateFlow<List<RestrictedApp>>(emptyList())
    val displayedApps: StateFlow<List<RestrictedApp>> = _displayedApps.asStateFlow()

    private val _userProgress = MutableStateFlow<UserProgress?>(null)
    val userProgress: StateFlow<UserProgress?> = _userProgress.asStateFlow()

    private val _activeSession = MutableStateFlow<UnlockSession?>(null)
    val activeSession: StateFlow<UnlockSession?> = _activeSession.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _todayScreenTime = MutableStateFlow("0m")
    val todayScreenTime: StateFlow<String> = _todayScreenTime.asStateFlow()

    private val _commonDailyLimitMillis = MutableStateFlow(0L)
    val commonDailyLimitMillis: StateFlow<Long> = _commonDailyLimitMillis.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _authEmail = MutableStateFlow<String?>(null)
    val authEmail: StateFlow<String?> = _authEmail.asStateFlow()

    private val _remoteTotalPoints = MutableStateFlow<Long?>(null)
    val remoteTotalPoints: StateFlow<Long?> = _remoteTotalPoints.asStateFlow()

    init {
        _authEmail.value = firebaseAuth.currentUser?.email
        firebaseAuth.addAuthStateListener { auth ->
            _authEmail.value = auth.currentUser?.email
            if (auth.currentUser != null) {
                viewModelScope.launch {
                    repository.ensureRemoteUserDoc()
                    syncPointsWithRemote()
                }
            } else {
                _remoteTotalPoints.value = null
            }
        }

        viewModelScope.launch {
            try {
                repository.initializeDefaultData()
                updateScreenTime()
            } catch (e: Exception) {
                Log.e("MindArcViewModel", "Error initializing data or updating screen time", e)
            } finally {
                _isInitialized.value = true
            }
        }

        viewModelScope.launch {
            repository.getAllApps().collect { apps ->
                _restrictedApps.value = apps
                loadAndMergeApps()
            }
        }

        viewModelScope.launch {
            repository.getProgress().collect { progress ->
                _userProgress.value = progress
            }
        }

        viewModelScope.launch {
            checkActiveSession()
        }

        viewModelScope.launch {
            _commonDailyLimitMillis.value = repository.getCommonDailyLimitMillis()
        }

        viewModelScope.launch {
            _userName.value = repository.getUserName()
        }
    }

    private suspend fun syncPointsWithRemote() {
        val local = repository.getProgressSync() ?: UserProgress()
        val remote = repository.getRemoteUserState()
        remote?.let { _remoteTotalPoints.value = it.totalPoints }
        val merged = maxOf(local.totalPoints.toLong(), remote?.totalPoints ?: 0L).toInt()
        if (merged != local.totalPoints) {
            repository.updateProgress(local.copy(totalPoints = merged))
        }
        repository.syncLocalTotalPointsToRemote(merged)
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val e = email.trim()
        if (e.isBlank() || password.isBlank()) {
            onResult(false, "Email and password required")
            return
        }
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(e, password).await()
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.message)
            }
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val e = email.trim()
        if (e.isBlank() || password.length < 6) {
            onResult(false, "Use a valid email and 6+ char password")
            return
        }
        viewModelScope.launch {
            try {
                firebaseAuth.createUserWithEmailAndPassword(e, password).await()
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.message)
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun signInWithGoogleIdToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cred = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(cred).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun completeOnboarding(
        name: String,
        goals: List<String>,
        dailyPhoneHours: Int
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            repository.setUserName(trimmedName)
            repository.setOnboardingGoals(goals)
            repository.setOnboardingDailyPhoneHours(dailyPhoneHours)
            repository.setOnboardingCompleted(true)
            _userName.value = trimmedName
        }
    }

    fun refreshUserName() {
        viewModelScope.launch {
            _userName.value = repository.getUserName()
        }
    }

    fun setCommonDailyLimitMillis(millis: Long) {
        viewModelScope.launch {
            repository.setCommonDailyLimitMillis(millis)
            _commonDailyLimitMillis.value = millis
        }
    }

    private var cachedInstalledApps: List<RestrictedApp>? = null
    private var installedAppsCacheTimeMs: Long = 0
    private val installedAppsCacheTtlMs = 30_000L

    private fun loadAndMergeApps() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val allInstalledApps = if (cachedInstalledApps != null && (now - installedAppsCacheTimeMs) < installedAppsCacheTtlMs) {
                cachedInstalledApps!!
            } else {
                withContext(Dispatchers.IO) { repository.getInstalledApps() }.also {
                    cachedInstalledApps = it
                    installedAppsCacheTimeMs = now
                }
            }
            val restrictedAppsFromDb = _restrictedApps.value
            val restrictedAppsMap = restrictedAppsFromDb.associateBy { it.packageName }
            val mergedList = allInstalledApps.map { installedApp ->
                restrictedAppsMap[installedApp.packageName]?.let { restrictedInfo ->
                    restrictedInfo.copy(usageTodayInMillis = installedApp.usageTodayInMillis)
                } ?: installedApp
            }
            _displayedApps.value = mergedList
        }
    }

    fun refreshDisplayedApps() {
        cachedInstalledApps = null
        loadAndMergeApps()
    }

    fun updateScreenTime() {
        viewModelScope.launch {
            try {
                // Keep the Home "Screen Time" card consistent with the dialog,
                // which shows social-media-only usage breakdown.
                val social = withContext(Dispatchers.IO) {
                    screenTimeManager.getTodaySocialMediaScreenTime()
                }
                _todayScreenTime.value = screenTimeManager.formatTime(social.totalMillis)
            } catch (e: Exception) {
                _todayScreenTime.value = "Need Permission"
            }
        }
    }

    /** Today's screen time for social media apps (WhatsApp, Instagram, etc.) that are installed. */
    suspend fun getSocialMediaScreenTime(): SocialMediaScreenTime = withContext(Dispatchers.IO) {
        screenTimeManager.getTodaySocialMediaScreenTime()
    }

    suspend fun loadInstalledApps(): List<RestrictedApp> {
        val now = System.currentTimeMillis()
        return if (cachedInstalledApps != null && (now - installedAppsCacheTimeMs) < installedAppsCacheTtlMs) {
            cachedInstalledApps!!
        } else {
            repository.getInstalledApps().also {
                cachedInstalledApps = it
                installedAppsCacheTimeMs = now
            }
        }
    }

    fun addRestrictedApp(app: RestrictedApp) {
        viewModelScope.launch {
            repository.insertApp(app.copy(isBlocked = true))
        }
    }

    fun removeRestrictedApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }

    fun toggleAppBlocked(app: RestrictedApp) {
        viewModelScope.launch {
            repository.updateApp(app.copy(isBlocked = !app.isBlocked))
        }
    }

    fun updateDailyLimit(packageName: String, limitInMillis: Long) {
        viewModelScope.launch {
            val app = _restrictedApps.value.find { it.packageName == packageName }
            app?.let {
                repository.updateApp(it.copy(dailyLimitInMillis = limitInMillis))
            }
        }
    }

    fun spendPointsToUnlock(points: Int, durationMinutes: Int = 15) {
        viewModelScope.launch {
            trySpendPointsToUnlock(points, durationMinutes)
        }
    }

    /** Returns true if redeem succeeded (balance was sufficient and session created). Call from a coroutine. */
    suspend fun trySpendPointsToUnlock(points: Int, durationMinutes: Int = 15): Boolean {
        val progress = repository.getProgressSync() ?: return false
        val remoteSpent = repository.spendRemotePointsAndSetUnlock(points, durationMinutes)
        if (!remoteSpent && progress.totalPoints < points) return false

        val updatedProgress = progress.copy(
            totalPoints = (progress.totalPoints - points).coerceAtLeast(0)
        )
        repository.updateProgress(updatedProgress)

        val activityId = repository.insertActivity(
            ActivityRecord(
                activityType = ActivityType.PUSHUPS,
                pointsEarned = -points,
                unlockDurationMinutes = durationMinutes
            )
        )

        val session = repository.createUnlockSession(activityId, durationMinutes)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()
        return true
    }

    suspend fun completePushupsActivity(pushups: Int): Long {
        val points = repository.calculatePoints(pushups)
        val unlockDuration = repository.calculateUnlockDuration(pushups)

        val activity = ActivityRecord(
            activityType = ActivityType.PUSHUPS,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completeSquatsActivity(squats: Int): Long {
        val points = repository.calculatePoints(squats)
        val unlockDuration = repository.calculateUnlockDuration(squats)

        val activity = ActivityRecord(
            activityType = ActivityType.SQUATS,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completeReadingActivity(
        activityType: ActivityType,
        readingMinutes: Int,
        readingContentId: Long? = null,
        userReadingTitle: String? = null
    ): Long {
        val points = repository.calculateReadingPoints(readingMinutes)
        val unlockDuration = repository.calculateReadingUnlockDuration(readingMinutes)

        val activity = ActivityRecord(
            activityType = activityType,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration,
            readingContentId = readingContentId,
            userReadingTitle = userReadingTitle
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(activity)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completeSpeedDialActivity(callDurationMinutes: Int): Long {
        // Fixed reward: 10 points + 10 minutes unlock for a 5+ minute call
        val points = 10
        val unlockDuration = 10

        val activity = ActivityRecord(
            activityType = ActivityType.SPEED_DIAL,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        repository.awardBadge(com.example.mindarc.data.model.Badge.HUMAN_CONNECTION)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completePongActivity(points: Int, unlockMinutes: Int): Long {
        val activity = ActivityRecord(
            activityType = ActivityType.PONG_GAME,
            pointsEarned = points,
            unlockDurationMinutes = unlockMinutes
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completeBreathingActivity(
        points: Int = 8,
        unlockMinutes: Int = 8
    ): Long {
        val activity = ActivityRecord(
            activityType = ActivityType.BREATHING,
            pointsEarned = points,
            unlockDurationMinutes = unlockMinutes
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun completePlankHoldActivity(secondsHeld: Int): Long {
        val points = repository.calculatePlankPoints(secondsHeld)
        val unlockDuration = repository.calculatePlankUnlockDurationMinutes(secondsHeld)

        val activity = ActivityRecord(
            activityType = ActivityType.PLANK_HOLD,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    /**
     * Claim a once-per-day reward based on steps walked today.
     * Returns true if claimed, false if already claimed today.
     */
    suspend fun claimStepsWalkedReward(stepsToday: Long): Boolean {
        if (!repository.canClaimStepsRewardToday()) return false

        val points = repository.calculateStepsPoints(stepsToday)
        val unlockDuration = repository.calculateStepsUnlockDurationMinutes(stepsToday)

        val activity = ActivityRecord(
            activityType = ActivityType.STEPS_WALKED,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        repository.markStepsRewardClaimedToday()
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return true
    }

    /** Trace-to-Earn: unlockMinutes = 5 (excellent), 1 (average), or 0 (needs improvement). */
    suspend fun completeTraceToEarnActivity(unlockMinutes: Int): Long {
        val points = when (unlockMinutes) {
            5 -> 5
            1 -> 1
            else -> 0
        }
        val activity = ActivityRecord(
            activityType = ActivityType.TRACE_TO_EARN,
            pointsEarned = points,
            unlockDurationMinutes = unlockMinutes
        )
        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        // Unlock only when user explicitly redeems points (spendPointsToUnlock)
        return activityId
    }

    suspend fun checkActiveSession() {
        repository.checkAndDeactivateExpiredSessions()
        _activeSession.value = repository.getActiveSession()
    }

    fun isAppUnlocked(packageName: String): Boolean {
        val session = _activeSession.value
        return session != null &&
               session.isActive &&
               System.currentTimeMillis() < session.endTime
    }
}
