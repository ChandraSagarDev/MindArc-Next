package com.example.mindarc.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.components.CompletionOverlay
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsWalkedActivityScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val healthConnectStatus = remember { HealthConnectClient.getSdkStatus(context) }
    val isHealthConnectAvailable = remember(healthConnectStatus) {
        healthConnectStatus == HealthConnectClient.SDK_AVAILABLE
    }

    val healthConnectClient = remember {
        if (isHealthConnectAvailable) HealthConnectClient.getOrCreate(context) else null
    }

    val stepsPermission = remember { HealthPermission.getReadPermission(StepsRecord::class) }
    var stepsPermissionGranted by remember { mutableStateOf(false) }

    var stepsToday by remember { mutableLongStateOf(0L) }
    var isReading by remember { mutableStateOf(false) }
    var claimState by remember { mutableStateOf<ClaimState>(ClaimState.Idle) }

    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult<Set<String>, Set<String>>(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        stepsPermissionGranted = grantedPermissions.contains(stepsPermission)
    }

    LaunchedEffect(isHealthConnectAvailable) {
        if (!isHealthConnectAvailable || healthConnectClient == null) return@LaunchedEffect
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        stepsPermissionGranted = granted.contains(stepsPermission)
    }

    LaunchedEffect(stepsPermissionGranted) {
        if (!stepsPermissionGranted || healthConnectClient == null) return@LaunchedEffect
        while (stepsPermissionGranted) {
            try {
                isReading = true
                val zone = ZoneId.systemDefault()
                val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
                val now = Instant.now()
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                stepsToday = response.records.sumOf { it.count }
            } catch (_: Exception) {
                // keep UI calm
            }
            isReading = false
            delay(15_000L)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Steps Walked",
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
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Steps)
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isHealthConnectAvailable) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Health data unavailable", fontWeight = FontWeight.Bold)
                            Text(
                                "Install/enable Health Connect to read steps.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                } else if (!stepsPermissionGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Connect Health", fontWeight = FontWeight.Bold)
                            Text(
                                "Grant steps permission to see today’s steps and claim a reward.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { permissionLauncher.launch(setOf(stepsPermission)) }) {
                                Text("Enable Steps Access")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
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
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (stepsPermissionGranted) "${stepsToday} steps" else "— steps",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isReading) "Reading…" else "Today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Claim once per day.\nMore steps = more unlock time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (!stepsPermissionGranted) return@Button
                        if (claimState == ClaimState.Claiming) return@Button
                        claimState = ClaimState.Claiming
                        scope.launch {
                            val claimed = viewModel.claimStepsWalkedReward(stepsToday)
                            claimState = if (claimed) ClaimState.Claimed else ClaimState.AlreadyClaimed
                        }
                    },
                    enabled = stepsPermissionGranted && claimState != ClaimState.Claimed && claimState != ClaimState.AlreadyClaimed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when (claimState) {
                            ClaimState.Idle -> "Claim reward"
                            ClaimState.Claiming -> "Claiming…"
                            ClaimState.Claimed -> "Claimed"
                            ClaimState.AlreadyClaimed -> "Already claimed today"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Back to Home")
                }
            }

            if (claimState == ClaimState.Claimed) {
                val points = maxOf(1, (stepsToday / 100L).toInt().coerceAtMost(200))
                val minutes = maxOf(1, (stepsToday / 200L).toInt().coerceAtMost(120))
                CompletionOverlay(
                    count = stepsToday.toInt(),
                    points = points,
                    duration = minutes,
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

private sealed interface ClaimState {
    data object Idle : ClaimState
    data object Claiming : ClaimState
    data object Claimed : ClaimState
    data object AlreadyClaimed : ClaimState
}

