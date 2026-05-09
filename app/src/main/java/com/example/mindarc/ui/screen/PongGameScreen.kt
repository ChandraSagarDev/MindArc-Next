package com.example.mindarc.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.theme.ActivityCardKind
import com.example.mindarc.ui.theme.ActivityScaffoldKind
import com.example.mindarc.ui.theme.activityCameraOverlayBrush
import com.example.mindarc.ui.theme.rememberActivityScaffoldBrush
import com.example.mindarc.ui.components.MindArcPrimaryButton
import com.example.mindarc.ui.components.MindArcSecondaryButton
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// ========================================================================
// DIFFICULTY SETTINGS
// ========================================================================
enum class PongDifficulty(
    val label: String,
    val description: String,
    val botSpeedFactor: Float,
    val botPaddleWidthFrac: Float,
    val ballSpeedFrac: Float,
    val rewardPoints: Int,
    val rewardUnlockMinutes: Int
) {
    EASY(
        label = "Easy",
        description = "Relaxed pace, wide bot paddle",
        botSpeedFactor = 0.35f,
        botPaddleWidthFrac = 0.28f,
        ballSpeedFrac = 0.32f,
        rewardPoints = 5,
        rewardUnlockMinutes = 5
    ),
    MEDIUM(
        label = "Medium",
        description = "Faster ball, smarter opponent",
        botSpeedFactor = 0.58f,
        botPaddleWidthFrac = 0.22f,
        ballSpeedFrac = 0.44f,
        rewardPoints = 10,
        rewardUnlockMinutes = 10
    ),
    HARD(
        label = "Hard",
        description = "Maximum speed, tiny bot paddle",
        botSpeedFactor = 0.82f,
        botPaddleWidthFrac = 0.16f,
        ballSpeedFrac = 0.56f,
        rewardPoints = 20,
        rewardUnlockMinutes = 20
    )
}

private const val WINNING_SCORE = 5
private const val PLAYER_PADDLE_WIDTH_FRAC = 0.24f
private const val PADDLE_HEIGHT_FRAC = 0.018f
private const val BALL_SIZE_FRAC = 0.055f
private const val PADDLE_MARGIN_FRAC = 0.07f

// ========================================================================
// LEVEL SELECTION SCREEN
// ========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongLevelSelectionScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Insta Pong", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Pong)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Difficulty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Beat the bot to $WINNING_SCORE to earn your reward. Harder = bigger reward.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            PongDifficulty.entries.forEach { difficulty ->
                DifficultyCard(
                    difficulty = difficulty,
                    onClick = {
                        navController.navigate(
                            Screen.PongGame.createRoute(difficulty.name.lowercase())
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        }
    }
}

@Composable
private fun DifficultyCard(difficulty: PongDifficulty, onClick: () -> Unit) {
    val (containerColor, accentColor) = when (difficulty) {
        PongDifficulty.EASY -> Color(0xFF4CAF50).copy(alpha = 0.10f) to Color(0xFF4CAF50)
        PongDifficulty.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.10f) to Color(0xFFFF9800)
        PongDifficulty.HARD -> Color(0xFFF44336).copy(alpha = 0.10f) to Color(0xFFF44336)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, accentColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = accentColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = difficulty.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${difficulty.rewardPoints} min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = "${difficulty.rewardUnlockMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ========================================================================
// PONG GAME SCREEN
// ========================================================================
@Composable
fun PongGameScreen(
    difficulty: String,
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val pongDifficulty = PongDifficulty.entries.find {
        it.name.equals(difficulty, ignoreCase = true)
    } ?: PongDifficulty.EASY

    val scope = rememberCoroutineScope()

    // --- Game state ---
    var ballX by remember { mutableStateOf(0.5f) }
    var ballY by remember { mutableStateOf(0.5f) }
    var ballVelX by remember { mutableStateOf(0f) }
    var ballVelY by remember { mutableStateOf(0f) }
    var playerPaddleX by remember { mutableStateOf(0.5f) }
    var botPaddleX by remember { mutableStateOf(0.5f) }
    var playerScore by remember { mutableStateOf(0) }
    var botScore by remember { mutableStateOf(0) }
    var pauseTimer by remember { mutableStateOf(1.0f) }
    var gameOver by remember { mutableStateOf(false) }
    var playerWon by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Key to restart the game loop (incrementing restarts LaunchedEffect)
    var gameKey by remember { mutableStateOf(0) }

    val ballSpeed = pongDifficulty.ballSpeedFrac
    val botSpeed = pongDifficulty.botSpeedFactor
    val botPaddleWidth = pongDifficulty.botPaddleWidthFrac
    val ballRadius = BALL_SIZE_FRAC / 2f
    val paddleHalfHeight = PADDLE_HEIGHT_FRAC / 2f
    val playerPaddleY = 1f - PADDLE_MARGIN_FRAC
    val botPaddleY = PADDLE_MARGIN_FRAC

    // --- Launch ball helper ---
    fun launchBall() {
        val angle = (Random.nextFloat() * 50f - 25f) * (PI.toFloat() / 180f)
        val dir = if (Random.nextBoolean()) 1f else -1f
        ballVelX = sin(angle) * ballSpeed * 0.6f
        ballVelY = cos(angle) * ballSpeed * dir
    }

    // --- Game loop ---
    LaunchedEffect(gameKey) {
        if (gameKey == 0) return@LaunchedEffect

        var lastNanos = 0L
        while (!gameOver) {
            withFrameNanos { nanos ->
                if (lastNanos == 0L) {
                    lastNanos = nanos
                    return@withFrameNanos
                }
                val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                lastNanos = nanos

                // --- Pause after score ---
                if (pauseTimer > 0f) {
                    pauseTimer -= dt
                    if (pauseTimer <= 0f) {
                        launchBall()
                    }
                    return@withFrameNanos
                }

                // --- Move ball ---
                ballX += ballVelX * dt
                ballY += ballVelY * dt

                // --- Wall bounces (left/right) ---
                if (ballX - ballRadius <= 0f) {
                    ballX = ballRadius
                    ballVelX = abs(ballVelX)
                }
                if (ballX + ballRadius >= 1f) {
                    ballX = 1f - ballRadius
                    ballVelX = -abs(ballVelX)
                }

                // --- Player paddle collision (bottom) ---
                val playerHalfW = PLAYER_PADDLE_WIDTH_FRAC / 2f
                if (ballVelY > 0 &&
                    ballY + ballRadius >= playerPaddleY - paddleHalfHeight &&
                    ballY - ballRadius <= playerPaddleY + paddleHalfHeight &&
                    ballX >= playerPaddleX - playerHalfW &&
                    ballX <= playerPaddleX + playerHalfW
                ) {
                    ballY = playerPaddleY - paddleHalfHeight - ballRadius
                    ballVelY = -abs(ballVelY)
                    val hitPos = (ballX - playerPaddleX) / playerHalfW
                    ballVelX += hitPos * ballSpeed * 0.5f
                    ballVelX = ballVelX.coerceIn(-ballSpeed * 1.5f, ballSpeed * 1.5f)
                }

                // --- Bot paddle collision (top) ---
                val botHalfW = botPaddleWidth / 2f
                if (ballVelY < 0 &&
                    ballY - ballRadius <= botPaddleY + paddleHalfHeight &&
                    ballY + ballRadius >= botPaddleY - paddleHalfHeight &&
                    ballX >= botPaddleX - botHalfW &&
                    ballX <= botPaddleX + botHalfW
                ) {
                    ballY = botPaddleY + paddleHalfHeight + ballRadius
                    ballVelY = abs(ballVelY)
                    val hitPos = (ballX - botPaddleX) / botHalfW
                    ballVelX += hitPos * ballSpeed * 0.5f
                    ballVelX = ballVelX.coerceIn(-ballSpeed * 1.5f, ballSpeed * 1.5f)
                }

                // --- Bot AI ---
                val maxBotMove = botSpeed * ballSpeed * 2.5f * dt
                val diff = ballX - botPaddleX
                botPaddleX += diff.coerceIn(-maxBotMove, maxBotMove)
                botPaddleX = botPaddleX.coerceIn(botHalfW, 1f - botHalfW)

                // --- Scoring ---
                if (ballY + ballRadius > 1.05f) {
                    // Ball passed player → bot scores
                    botScore++
                    ballX = 0.5f; ballY = 0.5f; ballVelX = 0f; ballVelY = 0f
                    pauseTimer = 0.8f
                } else if (ballY - ballRadius < -0.05f) {
                    // Ball passed bot → player scores
                    playerScore++
                    ballX = 0.5f; ballY = 0.5f; ballVelX = 0f; ballVelY = 0f
                    pauseTimer = 0.8f
                }

                // --- Game over ---
                if (playerScore >= WINNING_SCORE) {
                    gameOver = true; playerWon = true
                } else if (botScore >= WINNING_SCORE) {
                    gameOver = true; playerWon = false
                }
            }
        }
    }

    val pageBrush = rememberActivityScaffoldBrush(ActivityScaffoldKind.Pong)

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBrush)
    ) {
        // Game canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val w = size.width.toFloat()
                    val halfPW = PLAYER_PADDLE_WIDTH_FRAC / 2f
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).also { down ->
                            playerPaddleX =
                                (down.position.x / w).coerceIn(halfPW, 1f - halfPW)
                        }
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            if (pressed) {
                                event.changes.forEach { change ->
                                    playerPaddleX =
                                        (change.position.x / w).coerceIn(halfPW, 1f - halfPW)
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Dashed center line
            drawDashedLine(w, h)

            // Player paddle (bottom, blue)
            drawPaddle(
                centerX = playerPaddleX * w,
                centerY = playerPaddleY * h,
                width = PLAYER_PADDLE_WIDTH_FRAC * w,
                height = PADDLE_HEIGHT_FRAC * h,
                color = Color(0xFF4FC3F7)
            )

            // Bot paddle (top, red)
            drawPaddle(
                centerX = botPaddleX * w,
                centerY = botPaddleY * h,
                width = botPaddleWidth * w,
                height = PADDLE_HEIGHT_FRAC * h,
                color = Color(0xFFEF5350)
            )

            // Instagram ball
            drawInstagramBall(
                center = Offset(ballX * w, ballY * h),
                ballSize = BALL_SIZE_FRAC * w
            )
        }

        // Score display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$botScore",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFFEF5350).copy(alpha = 0.45f)
            )
            Text(
                text = "  :  ",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White.copy(alpha = 0.25f)
            )
            Text(
                text = "$playerScore",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4FC3F7).copy(alpha = 0.45f)
            )
        }

        // Labels
        Text(
            text = "BOT",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFEF5350).copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "YOU",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4FC3F7).copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )

        // Back button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White.copy(alpha = 0.6f))
        }

        // --- Start overlay ---
        if (gameKey == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = activityCameraOverlayBrush(
                            MaterialTheme.colorScheme,
                            ActivityCardKind.Pong
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${pongDifficulty.label} Mode",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "First to $WINNING_SCORE wins",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Drag to move your paddle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    MindArcPrimaryButton(
                        text = "Start game",
                        modifier = Modifier.fillMaxWidth(),
                        height = 52.dp,
                        hero = true,
                        onClick = { gameKey = 1 }
                    )
                }
            }
        }

        // --- Game over overlay ---
        if (gameOver && !isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconTint = if (playerWon) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        val icon: ImageVector = if (playerWon) Icons.Default.EmojiEvents
                            else Icons.Default.SportsEsports

                        Surface(
                            shape = CircleShape,
                            color = iconTint.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon, null,
                                    modifier = Modifier.size(36.dp),
                                    tint = iconTint
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (playerWon) "You Won!" else "Bot Wins!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "$playerScore - $botScore",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (playerWon) {
                            Text(
                                text = "+${pongDifficulty.rewardPoints} min earned • ${pongDifficulty.rewardUnlockMinutes} min unlock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        if (playerWon) {
                            MindArcPrimaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                height = 52.dp,
                                hero = true,
                                onClick = {
                                    scope.launch {
                                        viewModel.completePongActivity(
                                            pongDifficulty.rewardPoints,
                                            pongDifficulty.rewardUnlockMinutes
                                        )
                                        isCompleted = true
                                    }
                                },
                            ) {
                                Text("Claim reward", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MindArcSecondaryButton(
                                    modifier = Modifier
                                        .weight(1f),
                                    height = 52.dp,
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Text("Back", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                                MindArcPrimaryButton(
                                    modifier = Modifier
                                        .weight(1f),
                                    height = 52.dp,
                                    hero = true,
                                    onClick = {
                                        // Reset everything
                                        playerScore = 0; botScore = 0
                                        ballX = 0.5f; ballY = 0.5f
                                        ballVelX = 0f; ballVelY = 0f
                                        playerPaddleX = 0.5f; botPaddleX = 0.5f
                                        gameOver = false; playerWon = false
                                        pauseTimer = 1.0f
                                        gameKey++
                                    }
                                ) {
                                    Text("Retry", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Completion overlay ---
        if (isCompleted) {
            PongCompletionOverlay(
                points = pongDifficulty.rewardPoints,
                unlockMinutes = pongDifficulty.rewardUnlockMinutes,
                difficulty = pongDifficulty.label,
                onHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

// ========================================================================
// DRAWING HELPERS
// ========================================================================

/** Draw the Instagram-style icon as the Pong ball. */
private fun DrawScope.drawInstagramBall(center: Offset, ballSize: Float) {
    val halfSize = ballSize / 2f

    // Instagram gradient (bottom-left yellow → top-right purple)
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFEDA77),
            Color(0xFFF58529),
            Color(0xFFDD2A7B),
            Color(0xFF8134AF),
            Color(0xFF515BD4)
        ),
        start = Offset(center.x - halfSize, center.y + halfSize),
        end = Offset(center.x + halfSize, center.y - halfSize)
    )

    // Rounded square background
    drawRoundRect(
        brush = gradient,
        topLeft = Offset(center.x - halfSize, center.y - halfSize),
        size = Size(ballSize, ballSize),
        cornerRadius = CornerRadius(ballSize * 0.28f)
    )

    // Subtle inner border
    drawRoundRect(
        color = Color.White.copy(alpha = 0.2f),
        topLeft = Offset(center.x - halfSize, center.y - halfSize),
        size = Size(ballSize, ballSize),
        cornerRadius = CornerRadius(ballSize * 0.28f),
        style = Stroke(width = ballSize * 0.04f)
    )

    // Camera lens circle
    drawCircle(
        color = Color.White,
        radius = ballSize * 0.25f,
        center = center,
        style = Stroke(width = ballSize * 0.07f)
    )

    // Flash dot (top-right)
    drawCircle(
        color = Color.White,
        radius = ballSize * 0.065f,
        center = Offset(center.x + ballSize * 0.24f, center.y - ballSize * 0.24f)
    )
}

/** Draw a rounded paddle. */
private fun DrawScope.drawPaddle(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - width / 2f, centerY - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f)
    )
    // Glow effect
    drawRoundRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(centerX - width / 2f - 2f, centerY - height / 2f - 2f),
        size = Size(width + 4f, height + 4f),
        cornerRadius = CornerRadius(height / 2f + 2f)
    )
}

/** Draw a dashed horizontal center line. */
private fun DrawScope.drawDashedLine(width: Float, height: Float) {
    val dashLen = 14f
    val gapLen = 10f
    val y = height / 2f
    var x = 0f
    while (x < width) {
        drawRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(x, y - 1f),
            size = Size(dashLen, 2f)
        )
        x += dashLen + gapLen
    }
}

// ========================================================================
// COMPLETION OVERLAY
// ========================================================================
@Composable
private fun PongCompletionOverlay(
    points: Int,
    unlockMinutes: Int,
    difficulty: String,
    onHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
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
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Game Won!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "You beat the $difficulty bot! Earned $points minutes and unlocked your apps for $unlockMinutes minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Return Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
