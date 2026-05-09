package com.example.mindarc.utils

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reads system animator/transition scales to respect reduced-motion preferences.
 *
 * Android “reduce motion” is typically represented by setting the global animator/transition/window
 * animation scales to 0.0.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isReducedMotionEnabled() }
}

private fun Context.isReducedMotionEnabled(): Boolean {
    val resolver = contentResolver
    val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    val windowScale = Settings.Global.getFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
    return animatorScale == 0f || transitionScale == 0f || windowScale == 0f
}

