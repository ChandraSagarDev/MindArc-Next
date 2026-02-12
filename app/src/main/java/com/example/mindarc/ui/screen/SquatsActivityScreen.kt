package com.example.mindarc.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.processor.PoseDetectionProcessor
import com.example.mindarc.domain.PoseAnalyzer
import com.example.mindarc.ui.components.CameraPreview
import com.example.mindarc.ui.components.CompletionOverlay
import com.example.mindarc.ui.components.PermissionRequestUI
import com.example.mindarc.ui.components.PoseOverlay
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.viewmodel.SquatCounterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
    val scope = rememberCoroutineScope()

    val processor = remember {
        PoseDetectionProcessor(ActivityType.SQUATS) { metrics, pose, size ->
            if (metrics is PoseAnalyzer.SquatMetrics) {
                squatCounterViewModel.updateMetrics(metrics, pose, size)
            }
        }
    }

    val unlockDuration = remember(squatState.count) {
        if (squatState.count > 0) (squatState.count * 15) / 10 else 0
    }
    val points = remember(squatState.count) { squatState.count }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Squat Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionState.allPermissionsGranted) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    processor = processor
                )

                if (squatState.imageSize.width > 0) {
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
            } else {
                PermissionRequestUI(onGrant = { cameraPermissionState.launchMultiplePermissionRequest() })
            }

            if (cameraPermissionState.allPermissionsGranted && !isCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = squatState.formFeedback.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                squatState.formFeedback.contains("Good", true) -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                                !squatState.isGoodForm && squatState.isDetecting -> Color(0xFFF44336).copy(alpha = 0.9f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
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
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            4.dp,
                            if (squatState.isGoodForm) MaterialTheme.colorScheme.primary else Color.Gray
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
                        color = Color.Black.copy(alpha = 0.6f)
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
                                    Text("+$points", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("POINTS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                                VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(alpha = 0.2f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$unlockDuration min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("UNLOCK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
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
                                    modifier = Modifier.weight(1f).height(56.dp),
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

            if (isCompleted) {
                CompletionOverlay(squatState.count, points, unlockDuration) {
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
