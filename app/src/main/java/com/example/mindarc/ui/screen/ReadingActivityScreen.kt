package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingActivityScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reading Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Choose Your Experience",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Would you like to explore curated content or track your own reading session?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            ActivityCard(
                title = "Curated Articles",
                description = "Read short, insightful articles and test your knowledge with a quick quiz.",
                icon = Icons.Filled.MenuBook,
                onClick = { navController.navigate(Screen.AppProvidedReading.route) },
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                ),
                rewardText = "Earn 10+ Points"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActivityCard(
                title = "Personal Reading",
                description = "Read your own books or articles. Just set the timer and summarize later.",
                icon = Icons.Filled.AutoStories,
                onClick = { navController.navigate(Screen.UserProvidedReading.route) },
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                ),
                rewardText = "Earn 2 Points / Min"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
