package com.example.mindarc.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.ui.activities.BlockActivity
import com.example.mindarc.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockingService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventChannel = Channel<String>(Channel.UNLIMITED)

    private var currentForegroundApp: String? = null
    private var lastUsageUpdateTime: Long = 0

    private val countdownActive = mutableSetOf<String>()

    @Inject
    lateinit var repository: MindArcRepository

    private lateinit var notificationHelper: NotificationHelper

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_DATE_CHANGED) {
                serviceScope.launch {
                    repository.resetDailyStats()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppBlockingService", "Service connected")
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        registerReceiver(dateChangeReceiver, IntentFilter(Intent.ACTION_DATE_CHANGED))

        serviceScope.launch {
            repository.getAllApps().collect { apps ->
                currentForegroundApp?.let { checkAndBlockApp(it, apps) }
            }
        }

        serviceScope.launch {
            eventChannel.receiveAsFlow().collect { packageName ->
                checkAndBlockApp(packageName, repository.getAllApps().first())
            }
        }

        serviceScope.launch {
            while (true) {
                delay(1000)
                updateUsageStats {
                    currentForegroundApp?.let { app ->
                        checkAndBlockApp(app, repository.getAllApps().first())
                    }
                }
                repository.checkAndDeactivateExpiredSessions()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName != null) {
            val packageName = event.packageName.toString()
            if (packageName != currentForegroundApp) {
                updateUsageStats()
                currentForegroundApp = packageName
                lastUsageUpdateTime = System.currentTimeMillis()
                eventChannel.trySend(packageName)
            }
        }
    }

    private fun updateUsageStats(onUpdated: (suspend () -> Unit)? = null) {
        val app = currentForegroundApp
        val currentTime = System.currentTimeMillis()

        if (app != null && lastUsageUpdateTime > 0) {
            val usageDuration = currentTime - lastUsageUpdateTime
            serviceScope.launch {
                repository.updateUsage(app, usageDuration)
                onUpdated?.invoke()
            }
        }
        lastUsageUpdateTime = currentTime
    }

    override fun onInterrupt() {
        Log.d("AppBlockingService", "Service interrupted")
        updateUsageStats()
    }

    private suspend fun checkAndBlockApp(packageName: String, apps: List<RestrictedApp>) {
        val app = apps.find { it.packageName == packageName } ?: return
        val restrictedApp = app
        val totalUsageThisApp = restrictedApp.usageTodayInMillis
        val totalLimitThisApp = restrictedApp.dailyLimitInMillis + restrictedApp.extraTimePurchased
        val perAppUsageExceeded = restrictedApp.dailyLimitInMillis > 0 && totalUsageThisApp > totalLimitThisApp

        val commonLimit = repository.getCommonDailyLimitMillis()
        val blockedApps = apps.filter { it.isBlocked }
        val totalUsageAllBlocked = blockedApps.sumOf { it.usageTodayInMillis }
        val commonLimitExceeded = commonLimit > 0 && restrictedApp.isBlocked && totalUsageAllBlocked >= commonLimit

        val shouldBlock = when {
            commonLimit > 0 && restrictedApp.isBlocked -> commonLimitExceeded
            restrictedApp.dailyLimitInMillis > 0 -> perAppUsageExceeded
            else -> restrictedApp.isBlocked
        }

            if (shouldBlock) {
                val activeSession = repository.getActiveSession()
                val isUnlocked = activeSession != null &&
                        activeSession.isActive &&
                        System.currentTimeMillis() < activeSession.endTime

                if (!isUnlocked) {
                    countdownActive.remove(packageName)
                    blockApp(packageName, isCountdown = false)
                }
            } else if (restrictedApp.dailyLimitInMillis > 0) {
                val remainingTime = totalLimitThisApp - totalUsageThisApp

                if (remainingTime in 1..COUNTDOWN_DURATION_MS && !countdownActive.contains(packageName)) {
                    countdownActive.add(packageName)
                    val seconds = (remainingTime / 1000).toInt().coerceIn(1, 10)
                    blockApp(packageName, isCountdown = true, countdownSeconds = seconds)
                }
                else if (!restrictedApp.warningSent && remainingTime <= TimeUnit.MINUTES.toMillis(1)) {
                    notificationHelper.showWarningNotification(restrictedApp.appName, "1 minute")
                    val updatedApp = restrictedApp.copy(warningSent = true)
                    repository.updateApp(updatedApp)
                }
            }
    }

    private fun blockApp(packageName: String, isCountdown: Boolean = false, countdownSeconds: Int = 0) {
        try {
            val intent = Intent(this, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("packageName", packageName)
                putExtra("isCountdown", isCountdown)
                putExtra("countdownSeconds", countdownSeconds)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val COUNTDOWN_DURATION_MS = 10_000L
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AppBlockingService", "Service destroyed")
        updateUsageStats()
        unregisterReceiver(dateChangeReceiver)
        serviceScope.cancel()
    }
}
