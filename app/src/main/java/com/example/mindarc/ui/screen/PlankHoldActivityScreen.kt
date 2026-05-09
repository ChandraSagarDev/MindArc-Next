package com.example.mindarc.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.processor.PoseDetectionProcessor
import com.example.mindarc.domain.PoseAnalyzer
import com.example.mindarc.ui.components.CameraPreview
import com.example.mindarc.ui.components.CompletionOverlay
import com.example.mindarc.ui.components.PermissionRequestUI
import com.example.mindarc.ui.components.PoseOverlay
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.viewmodel.PlankHoldCounterViewModel
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PlankHoldActivityScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel(),
    plankHoldCounterViewModel: PlankHoldCounterViewModel = hiltViewModel()
) {
    val targetSeconds = 60
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var earnedPoints by remember { mutableIntStateOf(0) }
    var earnedUnlockMinutes by remember { mutableIntStateOf(0) }

    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.CAMERA)
    )

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val plankState by plankHoldCounterViewModel.state.collectAsState()

    val processor = remember {
        PoseDetectionProcessor(ActivityType.PLANK_HOLD) { metrics, pose, size ->
            if (metrics is PoseAnalyzer.PlankHoldMetrics) {
                plankHoldCounterViewModel.updateMetrics(metrics, pose, size)
            }
        }
    }

    val latestIsPlankCorrect by rememberUpdatedState(plankState.isCorrect)

    LaunchedEffect(isRunning, isCompleted) {
        if (!isRunning || isCompleted) return@LaunchedEffect
        while (isRunning && !isCompleted) {
            delay(1_000L)
            if (latestIsPlankCorrect) {
                elapsedSeconds++

                if (elapsedSeconds % 10 == 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                if (elapsedSeconds >= targetSeconds) {
                    isRunning = false
                    scope.launch {
                        viewModel.completePlankHoldActivity(secondsHeld = elapsedSeconds)
                        earnedPoints = maxOf(1, elapsedSeconds / 10)
                        earnedUnlockMinutes = maxOf(1, elapsedSeconds / 15).coerceAtMost(60)
                        isCompleted = true
                    }
                    return@LaunchedEffect
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Plank Hold",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Plank)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (cameraPermissionState.allPermissionsGranted) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    processor = processor
                )

                if ((isRunning || elapsedSeconds > 0) && plankState.imageSize.width > 0) {
                    PoseOverlay(
                        modifier = Modifier.fillMaxSize(),
                        pose = plankState.currentPose,
                        imageSize = plankState.imageSize,
                        repCount = elapsedSeconds,
                        depthPercentage = plankState.stabilityPercentage,
                        feedback = plankState.formFeedback,
                        primaryLabel = "seconds",
                        secondaryLabel = "form",
                        showSecondary = true
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.height(96.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${elapsedSeconds}s / ${targetSeconds}s",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (isCompleted) "Complete" else if (isRunning) plankState.formFeedback else "Ready?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Hold a plank for 60 seconds.\nFace the camera or stand sideways (profile) so your full body is visible.\nVibration cues every 10 seconds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (!isCompleted) {
                        MindArcPrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            hero = true,
                            onClick = {
                                val nextRunning = !isRunning
                                if (nextRunning && elapsedSeconds == 0) {
                                    processor.poseAnalyzer.resetReps()
                                    plankHoldCounterViewModel.reset()
                                }
                                isRunning = nextRunning
                            },
                            text = if (isRunning) "Pause" else "Start plank",
                        )
                    } else {
                        MindArcSecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            onClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            text = "Done \u2013 back to home",
                        )
                    }
                }
            } else {
                PermissionRequestUI(onGrant = { cameraPermissionState.launchMultiplePermissionRequest() })
            }

            if (isCompleted) {
                CompletionOverlay(
                    count = elapsedSeconds,
                    points = earnedPoints,
                    duration = earnedUnlockMinutes,
                    onHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        }
    }
}

