package com.example.mindarc.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.example.mindarc.utils.rememberReducedMotionEnabled

/**
 * Announces each rep count via TTS when the count increases (e.g. "One", "Two", "Three").
 * Call this from pushup or squat screens when the activity has started.
 */
@Composable
fun RepCountTts(
    currentCount: Int,
    hasStarted: Boolean
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var prevCount by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        var engineRef: TextToSpeech? = null
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engineRef?.language = Locale.getDefault()
                tts = engineRef
            }
        }
        engineRef = engine
        onDispose {
            engine.shutdown()
            tts = null
        }
    }

    LaunchedEffect(currentCount, hasStarted) {
        if (!hasStarted) return@LaunchedEffect
        if (currentCount == 0) {
            prevCount = 0
            return@LaunchedEffect
        }
        if (currentCount > prevCount) {
            prevCount = currentCount
            val word = numberToWord(currentCount)
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "rep_$currentCount")
        }
    }
}

private fun numberToWord(n: Int): String = when (n) {
    1 -> "One"
    2 -> "Two"
    3 -> "Three"
    4 -> "Four"
    5 -> "Five"
    6 -> "Six"
    7 -> "Seven"
    8 -> "Eight"
    9 -> "Nine"
    10 -> "Ten"
    11 -> "Eleven"
    12 -> "Twelve"
    13 -> "Thirteen"
    14 -> "Fourteen"
    15 -> "Fifteen"
    16 -> "Sixteen"
    17 -> "Seventeen"
    18 -> "Eighteen"
    19 -> "Nineteen"
    20 -> "Twenty"
    else -> n.toString()
}

@Composable
fun CompletionOverlay(
    count: Int,
    points: Int,
    duration: Int,
    averageBpm: Int? = null,
    onHome: () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val haptic = LocalHapticFeedback.current
    val checkPulse = remember { Animatable(1f) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            checkPulse.snapTo(1f)
            reveal.snapTo(1f)
            return@LaunchedEffect
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        checkPulse.snapTo(0.96f)
        checkPulse.animateTo(
            1.10f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
        checkPulse.animateTo(
            1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )

        // “Morph” reveal: card scales up from the bottom (where start CTAs live).
        reveal.snapTo(0f)
        reveal.animateTo(
            1f,
            animationSpec = spring(
                stiffness = 600f,
                dampingRatio = 0.75f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.85f)
                val p = reveal.value.coerceIn(0f, 1f)
                scaleX = 0.75f + (1f - 0.75f) * p
                scaleY = 0.75f + (1f - 0.75f) * p
                alpha = 0.7f + 0.3f * p
            }
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(checkPulse.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Incredible Work!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    text = "You smashed $count reps and unlocked your apps for $duration minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (averageBpm != null) {
                    Text(
                        text = "Average heart rate: $averageBpm bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.typography.bodyMedium.color,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Return Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionRequestUI(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "To count your reps accurately, MindArc uses AI to detect your posture through the camera.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onGrant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Enable Camera Access", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Small pill-shaped card that shows the user's current heart rate (bpm) during an active workout,
 * using Health Connect read access to HeartRateRecord.
 *
 * - If Health Connect is not available, a subtle \"Health data unavailable\" label is shown.
 * - If permissions are missing, a \"Connect Health\" chip allows the user to grant read access.
 * - When active and permitted, the chip polls recent heart rate samples every few seconds.
 */
@Composable
fun HeartRateChip(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onAverageBpmChanged: (Int?) -> Unit = {}
) {
    val context = LocalContext.current

    // Check once whether Health Connect is installed/available on this device.
    val healthConnectStatus = remember {
        HealthConnectClient.getSdkStatus(context)
    }
    val isHealthConnectAvailable = remember(healthConnectStatus) {
        healthConnectStatus == HealthConnectClient.SDK_AVAILABLE
    }

    if (!isHealthConnectAvailable) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
            )
        ) {
            Text(
                text = "Health data unavailable",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        return
    }

    val healthConnectClient = remember {
        HealthConnectClient.getOrCreate(context)
    }
    val bpmPermission = remember {
        HealthPermission.getReadPermission(HeartRateRecord::class)
    }
    val caloriesPermission = remember {
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    }
    // Full set we request from Health Connect, but BPM only strictly requires [bpmPermission].
    val allRequestedPermissions = remember {
        setOf(bpmPermission, caloriesPermission)
    }

    var bpmPermissionGranted by remember { mutableStateOf(false) }
    var caloriesPermissionGranted by remember { mutableStateOf(false) }
    var latestBpm by remember { mutableStateOf<Int?>(null) }
    var averageBpm by remember { mutableStateOf<Int?>(null) }
    var latestCaloriesKcal by remember { mutableStateOf<Double?>(null) }
    var isReading by remember { mutableStateOf(false) }

    // Launcher to request Health Connect read permissions.
    val permissionLauncher = rememberLauncherForActivityResult<Set<String>, Set<String>>(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        bpmPermissionGranted = grantedPermissions.contains(bpmPermission)
        caloriesPermissionGranted = grantedPermissions.contains(caloriesPermission)
    }

    // On first composition, check if we already have permission.
    LaunchedEffect(Unit) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        bpmPermissionGranted = granted.contains(bpmPermission)
        caloriesPermissionGranted = granted.contains(caloriesPermission)
    }

    // When workout is active and permission granted, poll recent heart rate samples.
    LaunchedEffect(isActive, bpmPermissionGranted, caloriesPermissionGranted) {
        if (!isActive || !bpmPermissionGranted) {
            isReading = false
            latestBpm = null
            averageBpm = null
            onAverageBpmChanged(null)
            return@LaunchedEffect
        }

        isReading = true
        while (isActive && bpmPermissionGranted) {
            try {
                val now = Instant.now()
                val twoMinutesAgo = now.minusSeconds(120)

                // Latest heart rate within the last couple of minutes.
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(twoMinutesAgo, now)
                    )
                )

                val latestSample = response.records
                    .flatMap { it.samples }
                    .maxByOrNull { it.time }

                latestBpm = latestSample?.beatsPerMinute?.toInt()

                // Update running average BPM over this workout session.
                latestBpm?.let { bpm ->
                    val newAvg = if (averageBpm == null) {
                        bpm
                    } else {
                        ((averageBpm!! + bpm) / 2.0).roundToInt()
                    }
                    averageBpm = newAvg
                    onAverageBpmChanged(newAvg)
                }

                // Aggregate recent active calories burned over the last ~2 hours,
                // but only if we actually have that permission.
                latestCaloriesKcal = if (caloriesPermissionGranted) {
                    val twoHoursAgo = now.minusSeconds(7_200)
                    val caloriesResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(twoHoursAgo, now)
                        )
                    )
                    val totalKcal = caloriesResponse.records.sumOf { it.energy.inKilocalories }
                    if (totalKcal > 0.0) totalKcal else null
                } else {
                    null
                }

            } catch (_: Exception) {
                // Swallow and keep UI calm; heart rate simply won't update.
            }

            delay(5_000L)
        }
        isReading = false
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasBpm = averageBpm != null
            val kcalText = latestCaloriesKcal?.let { "${it.roundToInt()} kcal" }

            Icon(
                imageVector = if (hasBpm) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (hasBpm) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            when {
                !bpmPermissionGranted -> {
                    TextButton(
                        onClick = { permissionLauncher.launch(allRequestedPermissions) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Connect Health",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                hasBpm -> {
                    Text(
                        text = "${averageBpm} bpm avg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (kcalText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = kcalText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                isReading -> {
                    Text(
                        text = "Reading…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                else -> {
                    Text(
                        text = "— bpm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

