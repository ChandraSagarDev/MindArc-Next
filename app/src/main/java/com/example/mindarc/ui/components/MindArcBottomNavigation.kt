package com.example.mindarc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mindarc.ui.navigation.Screen

/**
 * Bottom bar pattern from `redesigns/dashboard_violet_accent` / `activities_with_violet_accent_and_bgs`:
 * frosted bar, rounded top corners, pill highlight on the active tab.
 */
enum class MindArcMainTab {
    Home,
    Activities,
    Profile,
}

@Composable
fun MindArcBottomBar(
    selected: MindArcMainTab,
    onTabSelected: (MindArcMainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.2f),
            )
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = selected == MindArcMainTab.Home,
                onClick = { onTabSelected(MindArcMainTab.Home) }
            )
            BottomTabItem(
                label = "Activities",
                icon = Icons.Default.Timer,
                selected = selected == MindArcMainTab.Activities,
                onClick = { onTabSelected(MindArcMainTab.Activities) }
            )
            BottomTabItem(
                label = "Profile",
                icon = Icons.Default.Person,
                selected = selected == MindArcMainTab.Profile,
                onClick = { onTabSelected(MindArcMainTab.Profile) }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) primary.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) primary else muted,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = if (selected) primary else muted,
            fontSize = 10.sp
        )
    }
}

/**
 * Maps current route → tab; wires navigation with [launchSingleTop].
 */
@Composable
fun MindArcBottomBarForNav(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val selected = when (route) {
        Screen.Home.route -> MindArcMainTab.Home
        Screen.ActivitySelection.route -> MindArcMainTab.Activities
        Screen.Progress.route -> MindArcMainTab.Profile
        else -> MindArcMainTab.Home
    }
    MindArcBottomBar(
        selected = selected,
        onTabSelected = { tab ->
            when (tab) {
                MindArcMainTab.Home ->
                    navController.navigate(Screen.Home.route) {
                        launchSingleTop = true
                    }
                MindArcMainTab.Activities ->
                    navController.navigate(Screen.ActivitySelection.route) {
                        launchSingleTop = true
                    }
                MindArcMainTab.Profile ->
                    navController.navigate(Screen.Progress.route) {
                        launchSingleTop = true
                    }
            }
        },
    )
}
