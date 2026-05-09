package com.example.mindarc.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.mindarc.MainActivity
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.components.ErrorBanner
import com.example.mindarc.ui.components.GlassCard
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.theme.MindArcTheme
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MindArcRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
        val packageName = intent.getStringExtra("packageName")
        val isCountdown = intent.getBooleanExtra("isCountdown", false)
        val countdownSeconds = intent.getIntExtra("countdownSeconds", 10)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }

        setContent {
            MindArcTheme {
                var app by remember { mutableStateOf<RestrictedApp?>(null) }
                val viewModel: MindArcViewModel = hiltViewModel()
                val userProgress by viewModel.userProgress.collectAsState()
                val minutesNeeded = 10

                LaunchedEffect(Unit) {
                    app = packageName?.let { repository.getAppByPackageName(it) }
                }

                if (isCountdown) {
                    CountdownScreen(
                        appName = app?.appName ?: "This app",
                        initialSeconds = countdownSeconds,
                        onCountdownFinished = { finish() }
                    )
                } else {
                    BlockedScreen(
                        app = app,
                        userProgress = userProgress,
                        pointsNeeded = minutesNeeded,
                        onUnlockWithPoints = {
                            lifecycleScope.launch {
                                if (viewModel.trySpendPointsToUnlock(minutesNeeded)) {
                                    finish()
                                }
                                // If false, balance was insufficient; stay on block screen
                            }
                        },
                        onDoActivities = {
                            val intent = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("startDestination", Screen.ActivitySelection.route)
                            }
                            startActivity(intent)
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownScreen(
    appName: String,
    initialSeconds: Int,
    onCountdownFinished: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(initialSeconds) }

    val progress by animateFloatAsState(
        targetValue = secondsLeft.toFloat() / initialSeconds,
        animationSpec = tween(durationMillis = 900),
        label = "countdown_progress"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        onCountdownFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Time Almost Up!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "$appName will be blocked in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = when {
                        secondsLeft <= 3 -> MaterialTheme.colorScheme.error
                        secondsLeft <= 6 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsLeft",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        fontSize = 72.sp,
                        color = when {
                            secondsLeft <= 3 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                    Text(
                        text = "seconds",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Save your progress and close the app now to avoid interruption.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun BlockedScreen(
    app: RestrictedApp?,
    userProgress: UserProgress?,
    pointsNeeded: Int,
    onUnlockWithPoints: () -> Unit,
    onDoActivities: () -> Unit,
    onClose: () -> Unit
) {
    val isTimeBlocked = app?.dailyLimitInMillis != 0L
    val title = if (isTimeBlocked) "Time Limit Reached!" else "App is Blocked"
    val subtitle = if (isTimeBlocked)
        "You've used up your daily limit for ${app?.appName ?: "this app"}."
    else
        "${app?.appName ?: "This app"} is restricted. Complete an activity to unlock it."
    val currentPoints = userProgress?.totalPoints ?: 0
    val hasEnoughPoints = currentPoints >= pointsNeeded

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                containerAlpha = 0.45f,
                borderAlpha = 0.25f,
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Your Balance (minutes earned)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currentPoints min",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!hasEnoughPoints) {
                        Text(
                            text = "Short of ${pointsNeeded - currentPoints} min",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (hasEnoughPoints) {
                MindArcPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onUnlockWithPoints,
                    height = 56.dp,
                    hero = true,
                ) {
                    Text(
                        "Spend $pointsNeeded min to Unlock",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDoActivities
                ) {
                    Text("Earn Minutes with Activity Instead")
                }
            } else {
                ErrorBanner(
                    title = "Insufficient balance",
                    message = "Complete an activity to earn more minutes.",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                MindArcPrimaryButton(
                    text = "Go to Activities",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDoActivities,
                    height = 56.dp,
                    hero = true,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MindArcSecondaryButton(
                text = "Close",
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                onClick = onClose,
            )
        }
    }
}

