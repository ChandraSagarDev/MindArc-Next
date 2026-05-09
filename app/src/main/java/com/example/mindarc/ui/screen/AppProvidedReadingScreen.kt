package com.example.mindarc.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.viewmodel.ReadingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProvidedReadingScreen(navController: NavController) {
    val viewModel: ReadingViewModel = hiltViewModel()
    val readingContent by viewModel.readingContent.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()

    val totalTimeInSeconds = (readingContent?.estimatedReadingTimeMinutes ?: 0) * 60
    var timeLeft by remember { mutableIntStateOf(totalTimeInSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val isTimerFinished by remember { derivedStateOf { timeLeft <= 0 } }
    val requiredReadingTime = (totalTimeInSeconds * 0.4).toInt()
    val timeRead = totalTimeInSeconds - timeLeft
    val canTakeQuiz by remember { derivedStateOf { timeRead >= requiredReadingTime } }

    var hasLeftApp by remember { mutableStateOf(false) }
    var showFocusPenaltyDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isTimerRunning && !isTimerFinished) {
                hasLeftApp = true
                showFocusPenaltyDialog = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val isQuizFinished by remember { derivedStateOf { currentQuestionIndex >= quizQuestions.size } }

    // Active Recall state
    val paragraphs = remember(readingContent) { readingContent?.content?.split("\n\n") ?: emptyList() }
    var visibleParagraphsCount by remember { mutableIntStateOf(1) }
    var showRecallPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = isTimerRunning, key2 = timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
            
            // Trigger Active Recall prompts
            if (timeLeft % 45 == 0 && visibleParagraphsCount < paragraphs.size) {
                showRecallPrompt = true
                isTimerRunning = false
            }
        }
    }

    LaunchedEffect(totalTimeInSeconds) {
        timeLeft = totalTimeInSeconds
    }

    if (showFocusPenaltyDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Focus Penalty") },
            text = { Text("You left the app while reading. The 'Perfect Score' bonus has been voided for this session.") },
            confirmButton = {
                TextButton(onClick = { showFocusPenaltyDialog = false }) {
                    Text("Understood")
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        readingContent?.title ?: "Article", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.AppProvidedReading)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (readingContent != null) {
                if (!isTimerRunning && !isTimerFinished && !showRecallPrompt && visibleParagraphsCount == 1) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Ready to Read?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You'll have ${readingContent!!.estimatedReadingTimeMinutes} minutes to finish this article. Keep the app open to maintain your focus bonus!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = { isTimerRunning = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Reading", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                } else if ((isTimerRunning || showRecallPrompt || visibleParagraphsCount > 1) && !isTimerFinished) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = readingContent?.category ?: "Knowledge",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    LinearProgressIndicator(
                        progress = { timeLeft.toFloat() / totalTimeInSeconds },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        paragraphs.take(visibleParagraphsCount).forEach { para ->
                            Text(
                                text = para,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        if (showRecallPrompt) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Quick Check: Are you still focused?",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Reflect briefly on the last paragraph before continuing.")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            showRecallPrompt = false
                                            isTimerRunning = true
                                            visibleParagraphsCount++
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Continue Reading")
                                    }
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { timeLeft = 0 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canTakeQuiz,
                        colors = if (canTakeQuiz) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        if (canTakeQuiz) {
                            Text("Take Quiz Now", fontWeight = FontWeight.Bold)
                        } else {
                            val secondsRemaining = (requiredReadingTime - timeRead).coerceAtLeast(0)
                            Text("Read more to unlock quiz (${secondsRemaining}s left)")
                        }
                    }
                } else if (!isQuizFinished) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Knowledge Check",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Question ${currentQuestionIndex + 1} of ${quizQuestions.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        QuizQuestionView(
                            question = quizQuestions[currentQuestionIndex], 
                            selectedOption = selectedAnswer, 
                            onOptionSelected = { selectedAnswer = it }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                if (selectedAnswer == quizQuestions[currentQuestionIndex].correctAnswer) {
                                    score += 10
                                }
                                selectedAnswer = null
                                currentQuestionIndex++
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = selectedAnswer != null
                        ) {
                            Text(
                                if (currentQuestionIndex < quizQuestions.size - 1) "Next Question" else "Finish Quiz",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    val correctCount = score / 10
                    val wrongCount = (quizQuestions.size - correctCount).coerceAtLeast(0)
                    val isQuizPerfect = correctCount == quizQuestions.size

                    Spacer(modifier = Modifier.weight(0.5f))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    
                    Text(
                        "Correct: $correctCount   •   Wrong: $wrongCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    if (hasLeftApp) {
                        Text(
                            "Perfect Score bonus is voided for this session, because you left the app.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else if (isQuizPerfect) {
                        Text(
                            "Perfect Score! Your focus bonus is eligible—collect rewards below.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    } else {
                        Text(
                            "Collect your rewards. Your unlock time is based on reading duration and quiz performance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // Small helper copy: keeps the “earned” mental model clear.
                    Text(
                        "Tip: Staying in the app helps you keep Perfect Score eligibility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = {
                            viewModel.saveAppProvidedReading(
                                durationMinutes = totalTimeInSeconds / 60,
                                isQuizPerfect = isQuizPerfect,
                                hasLeftApp = hasLeftApp
                            )
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Collect Rewards", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        }
    }
}

@Composable
fun QuizQuestionView(question: QuizQuestion, selectedOption: String?, onOptionSelected: (String) -> Unit) {
    val options = remember(question) {
        listOf(question.option1, question.option2, question.option3, question.option4).shuffled()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = question.question, 
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        options.forEach { option ->
            val isSelected = option == selectedOption
            Surface(
                onClick = { onOptionSelected(option) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onOptionSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
