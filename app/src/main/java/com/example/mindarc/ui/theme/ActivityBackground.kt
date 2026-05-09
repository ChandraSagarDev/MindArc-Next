package com.example.mindarc.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Quieter, brand-aligned ambient backgrounds for activity flows.
 * Uses low-chroma blends so color reads as atmosphere, not decoration.
 */
enum class ActivityScaffoldKind {
    Selection,
    ReadingHub,
    AppProvidedReading,
    UserProvidedReading,
    Breathing,
    Steps,
    Plank,
    Trace,
    SpeedDial,
    Pong,
}

enum class ActivityCardKind {
    Pushups,
    Squats,
    Plank,
    Steps,
    ReadingCurated,
    ReadingPersonal,
    SpeedDial,
    Breathing,
    Trace,
    Pong,
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t,
    )
}

/** Full-screen vertical wash behind scaffold content (opaque stops). */
fun activityScaffoldBrush(scheme: ColorScheme, kind: ActivityScaffoldKind): Brush {
    val bg = scheme.background
    val midBase = scheme.surfaceContainerLow
    val (accent, accent2) = when (kind) {
        ActivityScaffoldKind.Selection ->
            scheme.primary to scheme.secondary
        ActivityScaffoldKind.ReadingHub,
        ActivityScaffoldKind.AppProvidedReading ->
            scheme.tertiary to scheme.primary
        ActivityScaffoldKind.UserProvidedReading ->
            scheme.secondary to scheme.tertiary
        ActivityScaffoldKind.Breathing ->
            Success to scheme.tertiary
        ActivityScaffoldKind.Steps ->
            Success to scheme.primary
        ActivityScaffoldKind.Plank ->
            scheme.secondary to scheme.primary
        ActivityScaffoldKind.Trace ->
            scheme.tertiary to scheme.secondary
        ActivityScaffoldKind.SpeedDial ->
            scheme.primary to scheme.tertiary
        ActivityScaffoldKind.Pong ->
            scheme.surfaceVariant to scheme.primary
    }
    val mid = lerpColor(lerpColor(midBase, accent, 0.10f), accent2, 0.06f)
    val upper = lerpColor(bg, mid, 0.55f)
    val lower = lerpColor(bg, mid, 0.35f)
    return Brush.verticalGradient(
        colors = listOf(bg, upper, mid, lower, bg),
    )
}

/** Soft linear fade inside activity cards (selection / reading hub). */
fun activityCardGradientColors(scheme: ColorScheme, kind: ActivityCardKind): List<Color> {
    val base = scheme.surface
    val (a, b) = when (kind) {
        ActivityCardKind.Pushups -> scheme.primary to scheme.secondary
        ActivityCardKind.Squats -> scheme.secondary to scheme.tertiary
        ActivityCardKind.Plank -> scheme.primary to scheme.tertiary
        ActivityCardKind.Steps -> Success to scheme.tertiary
        ActivityCardKind.ReadingCurated -> scheme.tertiary to scheme.primary
        ActivityCardKind.ReadingPersonal -> scheme.secondary to scheme.primary
        ActivityCardKind.SpeedDial -> scheme.tertiary to scheme.secondary
        ActivityCardKind.Breathing -> Success to scheme.tertiary
        ActivityCardKind.Trace -> scheme.secondary to scheme.primary
        ActivityCardKind.Pong -> scheme.surfaceVariant to scheme.primary
    }
    return listOf(
        lerpColor(base, a, 0.06f),
        lerpColor(base, b, 0.04f),
    )
}

/** Camera-session overlays: soft vertical tint so the preview shows through. */
fun activityCameraOverlayBrush(scheme: ColorScheme, kind: ActivityCardKind): Brush {
    val accent = when (kind) {
        ActivityCardKind.Pushups -> scheme.primary
        ActivityCardKind.Squats -> scheme.secondary
        ActivityCardKind.Pong -> scheme.primary
        else -> scheme.primary
    }
    return Brush.verticalGradient(
        colors = listOf(
            accent.copy(alpha = 0.12f),
            scheme.background.copy(alpha = 0.64f),
        ),
    )
}

/** Slightly denser scrim for countdown legibility. */
fun activityCameraCountdownBrush(scheme: ColorScheme, kind: ActivityCardKind): Brush {
    val accent = when (kind) {
        ActivityCardKind.Pushups -> scheme.primary
        ActivityCardKind.Squats -> scheme.secondary
        ActivityCardKind.Pong -> scheme.primary
        else -> scheme.primary
    }
    return Brush.verticalGradient(
        colors = listOf(
            lerpColor(accent.copy(alpha = 0.14f), scheme.background.copy(alpha = 0.55f), 0.45f),
            scheme.background.copy(alpha = 0.68f),
        ),
    )
}

/** Small panels (timer cards) — subtle tinted glass. */
fun activityPanelBrush(scheme: ColorScheme, accent: Color): Brush {
    val base = scheme.surfaceVariant
    return Brush.verticalGradient(
        colors = listOf(
            lerpColor(base, accent, 0.05f),
            lerpColor(base, accent, 0.03f),
        ),
    )
}

@Composable
fun rememberActivityScaffoldBrush(kind: ActivityScaffoldKind): Brush {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme, kind) { activityScaffoldBrush(scheme, kind) }
}
