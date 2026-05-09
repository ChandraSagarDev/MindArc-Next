package com.example.mindarc.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.components.MindArcTopAppBar
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.mindarc.ui.theme.Success

private const val MIN_CALL_DURATION_SECONDS = 5 * 60 // 5 minutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SpeedDialChallengeScreen(
    navController: NavController,
    mindArcViewModel: MindArcViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val callLogPermission = rememberPermissionState(Manifest.permission.READ_CALL_LOG)

    // Timestamp when the challenge was initiated
    var challengeStartTime by remember { mutableStateOf<Long?>(null) }
    // Whether we've verified the call
    var isVerified by remember { mutableStateOf(false) }
    // Call duration found (in minutes)
    var verifiedCallMinutes by remember { mutableIntStateOf(0) }
    // Verification in progress
    var isChecking by remember { mutableStateOf(false) }
    // Whether the reward was granted
    var isCompleted by remember { mutableStateOf(false) }
    // Feedback message
    var feedbackMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            MindArcTopAppBar(
                title = "Speed-Dial Challenge",
                transparentContainer = true,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.SpeedDial)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Call a Friend",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Make an outgoing call for at least 5 minutes to earn a Human Connection badge and 10 minutes of app time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Reward preview
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "+10",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "POINTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "10 min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "UNLOCK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Badge",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "HUMAN\nCONNECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // State-dependent content
                when {
                    !callLogPermission.status.isGranted -> {
                        // Need permission
                        Text(
                            text = "Call log access is needed to verify your call duration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MindArcPrimaryButton(
                            hero = true,
                            text = "Grant Call Log Permission",
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            onClick = { callLogPermission.launchPermissionRequest() }
                        )
                    }

                    challengeStartTime == null -> {
                        // Not started yet
                        Text(
                            text = "Tap below to start the challenge. The timer begins now — make your call and come back to verify.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MindArcPrimaryButton(
                            hero = true,
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            onClick = { challengeStartTime = System.currentTimeMillis() }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Challenge", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    isVerified && !isCompleted -> {
                        // Verified — show success and complete
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Success.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Call verified! $verifiedCallMinutes min outgoing call found.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Success
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        MindArcPrimaryButton(
                            hero = true,
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            onClick = {
                                scope.launch {
                                    mindArcViewModel.completeSpeedDialActivity(verifiedCallMinutes)
                                    isCompleted = true
                                }
                            },
                        ) {
                            Text("Claim Reward", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    !isVerified -> {
                        // Challenge started — show dialer + verify buttons
                        Text(
                            text = "Challenge active! Make a 5+ minute outgoing call, then come back and verify.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (feedbackMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = feedbackMessage,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Open dialer button
                        MindArcSecondaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL)
                                context.startActivity(intent)
                            },
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open dialer", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Verify call button
                        MindArcPrimaryButton(
                            hero = true,
                            modifier = Modifier.fillMaxWidth(),
                            height = 56.dp,
                            enabled = !isChecking,
                            onClick = {
                                isChecking = true
                                feedbackMessage = ""
                                scope.launch {
                                    val result = verifyCallLog(
                                        context,
                                        challengeStartTime ?: System.currentTimeMillis()
                                    )
                                    isChecking = false
                                    if (result != null && result >= MIN_CALL_DURATION_SECONDS) {
                                        isVerified = true
                                        verifiedCallMinutes = result / 60
                                    } else if (result != null) {
                                        val mins = result / 60
                                        val secs = result % 60
                                        feedbackMessage =
                                            "Longest call found: ${mins}m ${secs}s. Need at least 5 minutes."
                                    } else {
                                        feedbackMessage =
                                            "No outgoing calls found since you started the challenge. Make a call and try again."
                                    }
                                }
                            }
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking call log...", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify my call", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Completion overlay
            if (isCompleted) {
                SpeedDialCompletionOverlay(
                    callMinutes = verifiedCallMinutes,
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

/**
 * Queries the Android CallLog for the longest outgoing call made after [sinceTimestamp].
 * Returns the duration in seconds, or null if no outgoing calls are found.
 */
private fun verifyCallLog(context: Context, sinceTimestamp: Long): Int? {
    val projection = arrayOf(
        CallLog.Calls.TYPE,
        CallLog.Calls.DURATION,
        CallLog.Calls.DATE
    )
    val selection = "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.TYPE} = ?"
    val selectionArgs = arrayOf(
        sinceTimestamp.toString(),
        CallLog.Calls.OUTGOING_TYPE.toString()
    )
    val sortOrder = "${CallLog.Calls.DURATION} DESC"

    var longestDuration: Int? = null

    try {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
            if (cursor.moveToFirst() && durationIndex >= 0) {
                longestDuration = cursor.getInt(durationIndex)
            }
        }
    } catch (e: SecurityException) {
        // Permission not granted — shouldn't happen since we check, but just in case
        return null
    }

    return longestDuration
}

@Composable
private fun SpeedDialCompletionOverlay(callMinutes: Int, onHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Human Connection!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "You made a $callMinutes-minute call! You earned 10 minutes, the Human Connection badge, and unlocked your apps for 10 minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                MindArcSecondaryButton(
                    text = "Return home",
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                    onClick = onHome,
                )
            }
        }
    }
}
