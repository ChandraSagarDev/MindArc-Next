package com.example.mindarc.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.processor.PoseDetectionProcessor
import com.example.mindarc.domain.PoseAnalyzer
import com.example.mindarc.ui.components.CameraPreview
import com.example.mindarc.ui.components.CompletionOverlay
import com.example.mindarc.ui.components.HeartRateChip
import com.example.mindarc.ui.components.PermissionRequestUI
import com.example.mindarc.ui.components.PoseOverlay
import com.example.mindarc.ui.components.RepCountTts
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.viewmodel.SquatCounterViewModel
import com.example.mindarc.ui.theme.ActivityCardKind
import com.example.mindarc.ui.theme.Success
import com.example.mindarc.ui.theme.activityCameraCountdownBrush
import com.example.mindarc.ui.theme.activityCameraOverlayBrush
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.mindarc.utils.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SquatsActivityScreen(
    navController: NavController,
    mindArcViewModel: MindArcViewModel = hiltViewModel(),
    squatCounterViewModel: SquatCounterViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.CAMERA)
    )

    val squatState by squatCounterViewModel.state.collectAsState()
    var isCompleted by remember { mutableStateOf(false) }
    var averageHeartRate by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotionEnabled()

    // --- Start / Countdown state ---
    var hasStarted by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(5) }

    val processor = remember {
        PoseDetectionProcessor(ActivityType.SQUATS) { metrics, pose, size ->
            if (metrics is PoseAnalyzer.SquatMetrics) {
                squatCounterViewModel.updateMetrics(metrics, pose, size)
            }
        }
    }

    // Countdown timer
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            countdownValue = 5
            for (i in 5 downTo 1) {
                countdownValue = i
                delay(1000L)
            }
            countdownValue = 0 // 0 = "GO!"
            delay(600L)
            // Reset reps so any pre-start movement doesn't count
            processor.poseAnalyzer.resetReps()
            squatCounterViewModel.resetCount()
            isCountingDown = false
            hasStarted = true
        }
    }

    val unlockDuration = remember(squatState.count) {
        if (squatState.count > 0) (squatState.count * 15) / 10 else 0
    }
    // Each rep currently maps 1:1 to earned minutes in the UI.
    val points = remember(squatState.count) { squatState.count }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Squat Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionState.allPermissionsGranted) {
                // ---- Camera background (always running so user can position) ----
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    processor = processor
                )

                // Pose skeleton only after activity has started
                if (hasStarted && squatState.imageSize.width > 0) {
                    PoseOverlay(
                        modifier = Modifier.fillMaxSize(),
                        pose = squatState.currentPose,
                        imageSize = squatState.imageSize,
                        repCount = squatState.count,
                        depthPercentage = squatState.depthPercentage,
                        feedback = squatState.formFeedback
                    )
                }

                SquatGuideOverlay()

                // TTS: announce each rep ("One", "Two", ...) when count increases
                RepCountTts(currentCount = squatState.count, hasStarted = hasStarted)

                // Live heart rate (Health Connect)
                HeartRateChip(
                    isActive = hasStarted && !isCompleted,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    onAverageBpmChanged = { averageHeartRate = it }
                )

                // ============================================================
                // Phase 1 — NOT STARTED: show Start button
                // ============================================================
                if (!hasStarted && !isCountingDown && !isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AI SQUAT COUNTER",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Position your full body in frame and tap Start",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { isCountingDown = true },
                                modifier = Modifier
                                    .height(64.dp)
                                    .widthIn(max = 260.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ============================================================
                // Phase 2 — COUNTDOWN: 5 → 4 → 3 → 2 → 1 → GO!
                // ============================================================
                if (isCountingDown) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = activityCameraCountdownBrush(
                                    MaterialTheme.colorScheme,
                                    ActivityCardKind.Squats
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GET READY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (countdownValue > 0) "$countdownValue" else "GO!",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                                fontWeight = FontWeight.Black,
                            color = if (countdownValue > 0) Color.White else Success
                            )
                        }
                    }
                }

                // ============================================================
                // Phase 3 — ACTIVE: exercise UI
                // ============================================================
                if (hasStarted && !isCompleted) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(
                            visible = squatState.formFeedback.isNotEmpty(),
                            enter = if (reducedMotion) {
                                fadeIn()
                            } else {
                                fadeIn() + slideInVertically()
                            },
                            exit = if (reducedMotion) {
                                fadeOut()
                            } else {
                                fadeOut() + slideOutVertically()
                            }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    squatState.formFeedback.contains("Good", true) ->
                                        Success.copy(alpha = 0.9f)
                                    !squatState.isGoodForm && squatState.isDetecting ->
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                    else ->
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = squatState.formFeedback.uppercase(),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.size(160.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                4.dp,
                                if (squatState.isGoodForm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${squatState.count}",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "REPS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "+$points",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "MINUTES",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(32.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "$unlockDuration min",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            "UNLOCK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            processor.poseAnalyzer.resetReps()
                                            squatCounterViewModel.resetCount()
                                        },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            if (squatState.count > 0) {
                                                scope.launch {
                                                    mindArcViewModel.completeSquatsActivity(squatState.count)
                                                    isCompleted = true
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        enabled = squatState.count > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Finish Workout", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PermissionRequestUI(onGrant = { cameraPermissionState.launchMultiplePermissionRequest() })
            }

            // ---- Completion overlay ----
            if (isCompleted) {
                CompletionOverlay(squatState.count, points, unlockDuration, averageHeartRate) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
        }
    }
}

@Composable
fun SquatGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val strokeWidth = 2.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 0.7f),
            end = Offset(width, height * 0.7f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )

        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(width * 0.5f, height * 0.3f),
            end = Offset(width * 0.5f, height * 0.9f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}
