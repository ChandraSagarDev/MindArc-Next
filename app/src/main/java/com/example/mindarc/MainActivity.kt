package com.example.mindarc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.mindarc.ui.navigation.NavGraph
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.MindArcTheme
import com.example.mindarc.utils.hasUsageStatsPermission
import com.example.mindarc.utils.isAccessibilityServiceEnabled
import com.example.mindarc.utils.requestUsageStatsPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!hasUsageStatsPermission(this)) {
            requestUsageStatsPermission(this)
        }

        requestNotificationPermission()

        val prefs = getSharedPreferences("mindarc_prefs", MODE_PRIVATE)
        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        setContent {
            MindArcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val explicitStartDestination = intent.getStringExtra("startDestination")
                    val defaultStartDestination = if (!isOnboardingCompleted) {
                        Screen.Onboarding.route
                    } else if (isAccessibilityServiceEnabled(this) && Settings.canDrawOverlays(this)) {
                        Screen.Home.route
                    } else {
                        Screen.Permissions.route
                    }
                    val startDestination = explicitStartDestination ?: defaultStartDestination
                    NavGraph(navController = navController, startDestination = startDestination)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> { }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> { }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
