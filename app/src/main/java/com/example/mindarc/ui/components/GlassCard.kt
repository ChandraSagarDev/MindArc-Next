package com.example.mindarc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * "Glass" surface primitive: translucent surface + subtle border.
 *
 * This is intentionally token-driven (M3 roles) so screens do not hard-code colors.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    // Quieter baseline: slightly less “glassy” intensity by default.
    containerAlpha: Float = 0.78f,
    borderAlpha: Float = 0.12f,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            // `redesigns/*/code.html` glass-card: rgba(34,38,47,0.85) → surfaceContainerHighest @ ~85%
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = containerAlpha),
        ),
        border = BorderStroke(
            width = 1.dp,
            // ghost-border: outline-variant @ 15%
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha.coerceAtMost(0.2f)),
        ),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(contentPadding),
        ) {
            content()
        }
    }
}

