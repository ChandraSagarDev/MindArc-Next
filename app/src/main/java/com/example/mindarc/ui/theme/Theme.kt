package com.example.mindarc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Local composition for dark mode state management
val LocalDarkModePreference = compositionLocalOf<DarkModePreference> { 
    error("No DarkModePreference provided") 
}

enum class DarkModePreference {
    LIGHT, DARK, SYSTEM
}

// State holder for dark mode preference
class DarkModeState {
    var preference by mutableStateOf(DarkModePreference.DARK) // Changed default to DARK
}

val LocalDarkModeState = staticCompositionLocalOf<DarkModeState> { 
    error("No DarkModeState provided") 
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkTheme,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryGradientEnd,
    onPrimaryContainer = Color.White,
    secondary = SecondaryDarkTheme,
    onSecondary = Color(0xFF280067),
    secondaryContainer = Color(0xFF5516BE),
    onSecondaryContainer = Color(0xFFD9C8FF),
    tertiary = TertiaryDarkTheme,
    onTertiary = Color(0xFF003B56),
    tertiaryContainer = Color(0xFF34B5FA),
    onTertiaryContainer = Color(0xFF003047),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceDim = BackgroundDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = Color(0xFF73757D),
    outlineVariant = OutlineVariantDark,
    error = Error,
    onError = Color(0xFF490006),
    errorContainer = Color(0xFF9F0519),
    onErrorContainer = Color(0xFFFFA8A3),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMain,
    secondary = SecondaryMain,
    tertiary = TertiaryMain,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onTertiary = OnTertiary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError
)

@Composable
fun MindArcTheme(
    // Force darkTheme to true by default to fulfill the request
    darkTheme: Boolean = true, 
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = if (darkTheme) {
                BackgroundDark.toArgb()
            } else {
                Background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
