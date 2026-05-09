package com.example.mindarc.ui.screen

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.utils.isAccessibilityServiceEnabled
import com.example.mindarc.ui.components.MindArcPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingQuestionnaireScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }
    var dailyPhoneHours by remember { mutableIntStateOf(4) }

    val totalSteps = 4 // 0 intro, 1 name, 2 goals, 3 phone time
    val canContinue = when (step) {
        0 -> true
        1 -> name.trim().isNotBlank()
        2 -> selectedGoals.isNotEmpty()
        3 -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { ((step + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step > 0) step -= 1
                        },
                        enabled = step > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            when (step) {
                0 -> {
                    OnboardingIntro()
                }
                1 -> {
                    Text(
                        text = "First things first,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "What should we call you?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        label = { Text("Name") }
                    )
                }
                2 -> {
                    Text(
                        text = "What goals do you want to achieve using MindArc?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Choose up to 3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        "Reduce Screen Time",
                        "Quit late-night / early-morning scrolling",
                        "Build consistency & self-control",
                        "Better focus for study",
                        "Improved productivity at work"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.forEach { label ->
                            val isSelected = selectedGoals.contains(label)
                            SelectableGoalPill(
                                label = label,
                                selected = isSelected,
                                enabled = isSelected || selectedGoals.size < 3,
                                onToggle = {
                                    selectedGoals = if (isSelected) {
                                        selectedGoals - label
                                    } else {
                                        (selectedGoals + label).take(3).toSet()
                                    }
                                }
                            )
                        }
                    }
                }
                3 -> {
                    Text(
                        text = "Now let’s understand your phone usage a bit more.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "How much time do you spend on your phone every day?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "${dailyPhoneHours}h",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Slider(
                        value = dailyPhoneHours.toFloat(),
                        onValueChange = { dailyPhoneHours = it.toInt().coerceIn(0, 12) },
                        valueRange = 0f..12f,
                        steps = 11
                    )
                    Text(
                        text = "This helps us tune your recommendations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            MindArcPrimaryButton(
                hero = true,
                text = "Continue",
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    if (step < totalSteps - 1) {
                        step += 1
                    } else {
                        viewModel.completeOnboarding(
                            name = name,
                            goals = selectedGoals.toList(),
                            dailyPhoneHours = dailyPhoneHours
                        )

                        val next = if (isAccessibilityServiceEnabled(context) && Settings.canDrawOverlays(context)) {
                            Screen.Home.route
                        } else {
                            Screen.Permissions.route
                        }
                        navController.navigate(next) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingIntro() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Stars,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = "We know that quitting is hard.\n\nScience agrees — the best method is to replace.\n\nAnd what better replacement than exercise?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Backed by longitudinal studies, systematic reviews and behavioral science experts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SelectableGoalPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.22f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (selected) "✓" else "",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

