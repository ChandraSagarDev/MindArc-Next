package com.example.mindarc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column

import com.example.mindarc.ui.theme.Error
import com.example.mindarc.ui.theme.Success
import com.example.mindarc.ui.theme.Warning

enum class MindArcBannerTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

@Composable
fun MindArcBanner(
    tone: MindArcBannerTone,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val (bg, content, icon) = when (tone) {
        MindArcBannerTone.INFO -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Info,
        )
        MindArcBannerTone.SUCCESS -> Triple(
            Success.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.CheckCircle,
        )
        MindArcBannerTone.WARNING -> Triple(
            Warning.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Warning,
        )
        MindArcBannerTone.ERROR -> Triple(
            Error.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Error,
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when (tone) {
                    MindArcBannerTone.INFO -> MaterialTheme.colorScheme.primary
                    MindArcBannerTone.SUCCESS -> Success
                    MindArcBannerTone.WARNING -> Warning
                    MindArcBannerTone.ERROR -> Error
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.padding(top = 1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun InfoBanner(title: String, message: String, modifier: Modifier = Modifier) {
    MindArcBanner(tone = MindArcBannerTone.INFO, title = title, message = message, modifier = modifier)
}

@Composable
fun SuccessBanner(title: String, message: String, modifier: Modifier = Modifier) {
    MindArcBanner(tone = MindArcBannerTone.SUCCESS, title = title, message = message, modifier = modifier)
}

@Composable
fun WarningBanner(title: String, message: String, modifier: Modifier = Modifier) {
    MindArcBanner(tone = MindArcBannerTone.WARNING, title = title, message = message, modifier = modifier)
}

@Composable
fun ErrorBanner(title: String, message: String, modifier: Modifier = Modifier) {
    MindArcBanner(tone = MindArcBannerTone.ERROR, title = title, message = message, modifier = modifier)
}

