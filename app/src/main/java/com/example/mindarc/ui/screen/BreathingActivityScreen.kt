package com.example.mindarc.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.Success
import com.example.mindarc.ui.theme.activityPanelBrush
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingActivityScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val totalSeconds = 180 // 3-minute mindful breathing
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cycleLengthSeconds = 14 // 4 in, 4 hold, 6 out
    var cycleSecond by remember { mutableIntStateOf(0) }
    var currentPhase by remember { mutableStateOf("Ready") }
    var lastPhase by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRunning) {
        if (!isRunning || isCompleted) return@LaunchedEffect
        while (secondsLeft > 0 && isRunning) {
            delay(1000L)
            secondsLeft--
            cycleSecond = (cycleSecond + 1) % cycleLengthSeconds

            val phase = when {
                cycleSecond < 4 -> "Breathe in"
                cycleSecond < 8 -> "Hold"
                else -> "Breathe out"
            }
            if (phase != lastPhase) {
                currentPhase = phase
                lastPhase = phase
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        if (secondsLeft == 0 && !isCompleted) {
            scope.launch {
                // Award modest reward for low-screen mindfulness
                val points = 8
                val unlockMinutes = 8
                viewModel.completeBreathingActivity(points = points, unlockMinutes = unlockMinutes)
                isCompleted = true
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mindful Breathing",
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
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Breathing)
        val timerBrush = activityPanelBrush(MaterialTheme.colorScheme, Success)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.SelfImprovement,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Put your phone down,\nfollow your breath.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Breathe in for 4, hold for 4, breathe out for 6.\nKeep your eyes off the screen while the timer runs.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            val minutes = secondsLeft / 60
            val seconds = secondsLeft % 60

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(timerBrush)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            isCompleted -> "Session complete"
                            isRunning -> currentPhase
                            else -> "Ready?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isCompleted) {
                MindArcPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                    hero = true,
                    onClick = { isRunning = !isRunning },
                    text = if (isRunning) "Pause" else "Start 3-minute session",
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
                    text = "Done - back to home",
                )
            }
        }
        }
    }
}

