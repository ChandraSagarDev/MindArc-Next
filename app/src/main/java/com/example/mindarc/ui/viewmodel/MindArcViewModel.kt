package com.example.mindarc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.domain.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MindArcViewModel @Inject constructor(
    private val repository: MindArcRepository,
    private val screenTimeManager: ScreenTimeManager
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

    init {
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
    }

    private fun loadAndMergeApps() {
        viewModelScope.launch {
            val allInstalledApps = withContext(Dispatchers.IO) { repository.getInstalledApps() }
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
        loadAndMergeApps()
    }

    fun updateScreenTime() {
        viewModelScope.launch {
            try {
                val millis = withContext(Dispatchers.IO) {
                    screenTimeManager.getTodayTotalScreenTime()
                }
                _todayScreenTime.value = screenTimeManager.formatTime(millis)
            } catch (e: Exception) {
                _todayScreenTime.value = "Need Permission"
            }
        }
    }

    suspend fun loadInstalledApps(): List<RestrictedApp> {
        return repository.getInstalledApps()
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
            val progress = repository.getProgressSync() ?: return@launch
            if (progress.totalPoints >= points) {
                val updatedProgress = progress.copy(
                    totalPoints = progress.totalPoints - points
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
            }
        }
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

        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()

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

        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()

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

        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()

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
