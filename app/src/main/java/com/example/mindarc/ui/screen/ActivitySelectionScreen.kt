package com.example.mindarc.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.components.GlassCard
import com.example.mindarc.ui.components.MindArcBottomBarForNav
import com.example.mindarc.ui.components.MindArcBrandedTopAppBar
import com.example.mindarc.ui.theme.ActivityCardKind
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.activityCardGradientColors
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySelectionScreen(navController: NavController) {
    Scaffold(
        topBar = {
            MindArcBrandedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        bottomBar = { MindArcBottomBarForNav(navController) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Selection)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Select Activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Choose your focus for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            ActivityGroup(
                title = "Physical",
                subtitle = "Move your body to earn unlock time."
            ) {
                ActivityCard(
                    title = "AI Pushups",
                    description = "Build physical strength with AI-powered rep counting.",
                    icon = Icons.Filled.FitnessCenter,
                    onClick = { navController.navigate(Screen.PushupsActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Pushups
                    ),
                    rewardText = "1 min / rep"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "AI Squats",
                    description = "Strengthen your lower body with automated squat detection.",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    onClick = { navController.navigate(Screen.SquatsActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Squats
                    ),
                    rewardText = "1 min / rep"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "Plank Hold",
                    description = "60s forearm plank — front or side camera view. Vibration cues every 10s.",
                    icon = Icons.Filled.Timer,
                    onClick = { navController.navigate(Screen.PlankHoldActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Plank
                    ),
                    rewardText = "Up to 60 min"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "Steps Walked",
                    description = "Read today’s steps from Health Connect and claim a daily reward.",
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    onClick = { navController.navigate(Screen.StepsWalkedActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Steps
                    ),
                    rewardText = "Daily claim"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ActivityGroup(
                title = "Non‑physical",
                subtitle = "Calm your mind, connect, or create."
            ) {
                ActivityCard(
                    title = "Mindful Reading",
                    description = "Expand your knowledge with curated articles and summaries.",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { navController.navigate(Screen.ReadingActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.ReadingCurated
                    ),
                    rewardText = "2 min / min"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "Speed-Dial Challenge",
                    description = "Call a friend for 5+ minutes instead of texting or scrolling.",
                    icon = Icons.Filled.Phone,
                    onClick = { navController.navigate(Screen.SpeedDialChallenge.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.SpeedDial
                    ),
                    rewardText = "10 min + badge"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "Mindful Breathing",
                    description = "Take a 3-minute guided breathing break away from your screen.",
                    icon = Icons.Filled.SelfImprovement,
                    onClick = { navController.navigate(Screen.BreathingActivity.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Breathing
                    ),
                    rewardText = "8 min • 8 min"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActivityCard(
                    title = "Trace-to-Earn",
                    description = "Trace the outline in 60 seconds. Stay close to the line to earn social media time.",
                    icon = Icons.Filled.Brush,
                    onClick = { navController.navigate(Screen.TraceToEarn.route) },
                    gradientColors = activityCardGradientColors(
                        MaterialTheme.colorScheme,
                        ActivityCardKind.Trace
                    ),
                    rewardText = "1-5 min unlock"
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                containerAlpha = 0.22f,
                borderAlpha = 0.14f,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "More activities coming soon!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun ActivityGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    // No hard borders — tonal surface only (`redesigns/mindarc_obsidian/DESIGN.md`)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun ActivityCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    gradientColors: List<Color>,
    rewardText: String
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "card_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(56.dp).shadow(1.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = rewardText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Start",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
