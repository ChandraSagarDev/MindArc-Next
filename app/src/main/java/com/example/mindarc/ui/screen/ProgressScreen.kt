package com.example.mindarc.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.components.MindArcBottomBarForNav
import com.example.mindarc.ui.components.MindArcTopAppBar
import com.example.mindarc.ui.components.StatCard
import com.example.mindarc.viewmodel.ProgressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController) {
    val viewModel: ProgressViewModel = hiltViewModel()
    val userProgress by viewModel.userProgress.collectAsState()

    Scaffold(
        topBar = {
            MindArcTopAppBar(
                title = "Profile",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
            )
        },
        bottomBar = { MindArcBottomBarForNav(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Minutes Earned",
                    value = "${userProgress?.totalPoints ?: 0}",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
                StatCard(
                    title = "Current Streak",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Longest Streak",
                    value = "${userProgress?.longestStreak ?: 0}d",
                    icon = Icons.Default.MilitaryTech,
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                    )
                )
                StatCard(
                    title = "Activities",
                    value = "${userProgress?.totalActivities ?: 0}",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Milestones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            userProgress?.let { progress ->
                AchievementCard(
                    title = "Early Adopter",
                    description = "Started your journey with MindArc.",
                    achieved = progress.totalActivities > 0,
                    icon = Icons.Default.RocketLaunch
                )
                Spacer(modifier = Modifier.height(12.dp))
                AchievementCard(
                    title = "Consistency King",
                    description = "Maintained a 7-day streak.",
                    achieved = progress.longestStreak >= 7,
                    icon = Icons.Default.Diamond
                )
                Spacer(modifier = Modifier.height(12.dp))
                AchievementCard(
                    title = "Century Club",
                    description = "Earned over 100 minutes.",
                    achieved = progress.totalPoints >= 100,
                    icon = Icons.Default.Stars
                )
                Spacer(modifier = Modifier.height(12.dp))
                AchievementCard(
                    title = "Master of Focus",
                    description = "Unlocked apps 50 times.",
                    achieved = progress.totalUnlockSessions >= 50,
                    icon = Icons.Default.Psychology
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AchievementCard(
    title: String,
    description: String,
    achieved: Boolean,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (achieved) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (achieved) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (achieved) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (achieved) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (achieved) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (achieved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Achieved",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
