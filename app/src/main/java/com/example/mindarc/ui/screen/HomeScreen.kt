package com.example.mindarc.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.mindarc.domain.SocialMediaScreenTime
import com.example.mindarc.domain.getSocialMediaUsageTier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.components.MindArcBottomBarForNav
import com.example.mindarc.ui.components.MindArcBrandedTopAppBar
import com.example.mindarc.ui.components.MindArcPermissionCard
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.components.mindArcPillLabelTextStyle
import com.example.mindarc.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userProgress by viewModel.userProgress.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    val screenTime by viewModel.todayScreenTime.collectAsState()
    val userName by viewModel.userName.collectAsState()
    var showSocialMediaDialog by remember { mutableStateOf(false) }
    var socialMediaData by remember { mutableStateOf<SocialMediaScreenTime?>(null) }

    // Check for "Display over other apps" permission
    var hasOverlayPermission by remember { 
        mutableStateOf(Settings.canDrawOverlays(context)) 
    }

    LaunchedEffect(Unit) {
        viewModel.checkActiveSession()
        viewModel.updateScreenTime()
    }

    // Update permission status when returning to app
    DisposableEffect(Unit) {
        val observer = { hasOverlayPermission = Settings.canDrawOverlays(context) }
        onDispose { }
    }

    Scaffold(
        topBar = {
            MindArcBrandedTopAppBar {
                IconButton(onClick = { navController.navigate(Screen.Auth.route) }) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { navController.navigate(Screen.Progress.route) }) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = "Insights",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        bottomBar = { MindArcBottomBarForNav(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!isInitialized) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Permission Warning Card
                if (!hasOverlayPermission) {
                    MindArcPermissionCard(
                        title = "Permission required",
                        description = "We use 'Display over other apps' so MindArc can show the block screen when you open restricted apps.",
                        icon = Icons.Default.Warning,
                        actionText = "Grant permission",
                        onAction = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Ready to focus?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (!userName.isNullOrBlank()) {
                        "Welcome back, ${userName}! Keep building your streak."
                    } else {
                        "Track minutes earned and stay on top of your goals."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Active Session Card
                activeSession?.let { session ->
                    val currentTime = System.currentTimeMillis()
                    val remainingTime = (session.endTime - currentTime) / 1000 / 60
                    if (remainingTime > 0) {
                        ActiveSessionCard(remainingTime = remainingTime)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Quick Stats Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { navController.navigate(Screen.Progress.route) }) {
                        Text("Details")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // First Row of Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Minutes Earned",
                        value = "${userProgress?.totalPoints ?: 0}",
                        icon = Icons.Default.EmojiEvents,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                    StatCard(
                        title = "Streak",
                        value = "${userProgress?.currentStreak ?: 0}d",
                        icon = Icons.Default.Whatshot,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Second Row of Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Screen Time",
                        value = screenTime,
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f).then(
                            if (screenTime == "Need Permission") {
                                Modifier.shadow(4.dp, RoundedCornerShape(16.dp))
                            } else Modifier
                        ),
                        onClick = {
                            if (screenTime == "Need Permission") {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } else {
                                showSocialMediaDialog = true
                            }
                        },
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.info().copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.info().copy(alpha = 0.05f)
                        )
                    )
                    StatCard(
                        title = "Blocked",
                        value = "${restrictedApps.count { it.isBlocked }}",
                        icon = Icons.Default.AppBlocking,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.error.copy(alpha = 0.04f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                MindArcPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    height = 64.dp,
                    hero = true,
                    onClick = { navController.navigate(Screen.ActivitySelection.route) },
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Start Activity",
                        style = mindArcPillLabelTextStyle(FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                MindArcSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    height = 64.dp,
                    onClick = { navController.navigate(Screen.AppSelection.route) },
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Manage restricted apps",
                        style = mindArcPillLabelTextStyle(FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Social media screen time dialog (when Screen Time card is clicked and permission granted)
    LaunchedEffect(showSocialMediaDialog) {
        if (showSocialMediaDialog) {
            socialMediaData = viewModel.getSocialMediaScreenTime()
        }
    }
    if (showSocialMediaDialog) {
        SocialMediaScreenTimeDialog(
            data = socialMediaData,
            onDismiss = {
                showSocialMediaDialog = false
                socialMediaData = null
            },
            formatTime = { millis ->
                val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
                val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis) % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m"
                    else -> "0m"
                }
            }
        )
    }
}

@Composable
private fun SocialMediaScreenTimeDialog(
    data: SocialMediaScreenTime?,
    onDismiss: () -> Unit,
    formatTime: (Long) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Social media today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (data == null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                val tier = getSocialMediaUsageTier(data.totalMillis)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tier.emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Column {
                            Text(
                                text = tier.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total: ${formatTime(data.totalMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (data.appUsages.isEmpty()) {
                        Text(
                            text = "No social media apps with usage found on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "By app:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        data.appUsages.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = formatTime(entry.usageMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Helper to provide an 'info' color since it's not standard in Material3
@Composable
fun ColorScheme.info(): Color = tertiary

@Composable
fun ActiveSessionCard(remainingTime: Long) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Apps are Unlocked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$remainingTime",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
