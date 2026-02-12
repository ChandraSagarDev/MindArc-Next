package com.example.mindarc.service.blocking

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.mindarc.ui.activities.BlockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject
    lateinit var blockCache: BlockCache

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var lastBlockedTime: Long = 0
    private var screenOffReceiver: BroadcastReceiver? = null
    private var userPresentReceiver: BroadcastReceiver? = null

    private var isScreenOn = true

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isScreenOn || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        val safeList = setOf(
            "com.google.android.inputmethod.latin", // Gboard
            "com.android.systemui",
            this.packageName
        )

        if (safeList.contains(packageName)) {
            return
        }

        if (blockCache.shouldBlock(packageName)) {
            performBlock(packageName)
        }
    }

    private fun performBlock(packageName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockedTime > 2000) { // Debounce
            lastBlockedTime = currentTime

            performGlobalAction(GLOBAL_ACTION_HOME)

            val intent = Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("packageName", packageName)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceivers()
    }

    private fun registerReceivers() {
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isScreenOn = false
            }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        userPresentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isScreenOn = true
                serviceScope.launch {
                    blockCache.rebuildCache()
                }
            }
        }
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        unregisterReceiver(screenOffReceiver)
        unregisterReceiver(userPresentReceiver)
    }
}
