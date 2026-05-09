package com.example.mindarc.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.components.ErrorBanner
import com.example.mindarc.ui.components.GlassCard
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.viewmodel.MindArcViewModel

@Composable
fun LockWarningScreen(
    navController: NavController,
    packageName: String,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val userProgress by viewModel.userProgress.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val scope = rememberCoroutineScope()

    val minutesNeeded = 10
    val hasEnoughMinutes = (userProgress?.totalPoints ?: 0) >= minutesNeeded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = if (hasEnoughMinutes) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (hasEnoughMinutes) Icons.Default.Lock else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (hasEnoughMinutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "App Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You are trying to open a restricted app. You can unlock it for 15 minutes by spending $minutesNeeded earned minutes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Balance Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            containerAlpha = 0.45f,
            borderAlpha = 0.25f,
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Your Balance (minutes earned)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${userProgress?.totalPoints ?: 0} min",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!hasEnoughMinutes) {
                    Text(
                        text = "Short of ${minutesNeeded - (userProgress?.totalPoints ?: 0)} min",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (hasEnoughMinutes) {
            MindArcPrimaryButton(
                hero = true,
                onClick = {
                    scope.launch {
                        if (viewModel.trySpendPointsToUnlock(minutesNeeded)) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                height = 56.dp,
            ) {
                Text(
                    "Spend $minutesNeeded min to Unlock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { 
                    navController.navigate(Screen.ActivitySelection.route) 
                }
            ) {
                Text("Earn Minutes with Activity Instead")
            }
        } else {
            ErrorBanner(
                title = "Insufficient balance",
                message = "Complete an activity to earn more minutes.",
                modifier = Modifier.fillMaxWidth(),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            MindArcPrimaryButton(
                hero = true,
                text = "Go to Activities",
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                onClick = { navController.navigate(Screen.ActivitySelection.route) },
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MindArcSecondaryButton(
            text = "Cancel",
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
            onClick = { navController.navigate(Screen.Home.route) },
        )
    }
}
