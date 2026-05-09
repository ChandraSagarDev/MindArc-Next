package com.example.mindarc.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.mindarc.utils.rememberReducedMotionEnabled

/** Consistent label style for pill buttons (avoids top-heavy text from Android font padding). */
@Composable
fun mindArcPillLabelTextStyle(fontWeight: FontWeight = FontWeight.Bold) =
    MaterialTheme.typography.titleMedium.merge(
        TextStyle(
            fontWeight = fontWeight,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    )

@Composable
fun MindArcPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    shape: RoundedCornerShape = RoundedCornerShape(9999.dp),
    hero: Boolean = false,
    onClick: () -> Unit,
) {
    MindArcPrimaryButton(
        modifier = modifier,
        enabled = enabled,
        height = height,
        shape = shape,
        hero = hero,
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                )
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun MindArcPrimaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    shape: RoundedCornerShape = RoundedCornerShape(9999.dp),
    hero: Boolean = false,
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed) {
        if (hero) 0.985f else 0.99f
    } else {
        1f
    }
    val pressScale = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 120,
            easing = FastOutSlowInEasing
        ),
        label = "MindArcPrimaryButtonPressScale"
    ).value

    val primary = MaterialTheme.colorScheme.primary
    val primaryEnd = MaterialTheme.colorScheme.primaryContainer

    // `redesigns/dashboard_violet_accent`: ~135° from primary → deeper violet
    val gradientBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to if (enabled) primary else primary.copy(alpha = 0.5f),
            1f to if (enabled) primaryEnd else primaryEnd.copy(alpha = 0.5f),
        ),
        start = Offset.Zero,
        end = Offset(500f, 500f),
    )

    Box(
        modifier = modifier
            .height(height)
            .shadow(
                elevation = if (hero) 12.dp else 8.dp,
                shape = shape,
                spotColor = primary.copy(alpha = if (hero) 0.35f else 0.25f),
                ambientColor = Color.Black.copy(alpha = if (hero) 0.35f else 0.25f),
            )
            .clip(shape)
            .scale(pressScale)
            .background(gradientBrush)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun MindArcSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    shape: RoundedCornerShape = RoundedCornerShape(9999.dp),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height),
        enabled = enabled,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                )
            ),
        )
    }
}

@Composable
fun MindArcSecondaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    shape: RoundedCornerShape = RoundedCornerShape(9999.dp),
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height),
        enabled = enabled,
        shape = shape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
            top = 0.dp,
            bottom = 0.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

