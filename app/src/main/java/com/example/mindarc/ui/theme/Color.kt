package com.example.mindarc.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens inspired by `mindarc_new_design reference_/src/index.css`.
// Theme intent: calm, glassy dark UI with a purple primary and bright, readable accents.

// Primary palette (shared across light/dark)
val PrimaryMain = Color(0xFFA855F7)      // #a855f7
val SecondaryMain = Color(0xFFAC8AFF)    // #ac8aff
val TertiaryMain = Color(0xFF61C2FF)     // #61c2ff

// Status semantics (used by the component library)
val Success = Color(0xFF34D399)          // emerald-ish
val Warning = Color(0xFFFFD93D)          // amber/yellow
// `redesigns/*` + mindarc_obsidian: alarm red for blocked/errors
val Error = Color(0xFFFF716C)            // #ff716c
val Info = TertiaryMain

// Light mode (only to keep app functional; the reference is dark-forward)
val Background = Color(0xFFF7F8FF)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFEFF2FA)

val OnPrimary = Color.White
val OnSecondary = Color.White
val OnTertiary = Color(0xFF0B0E14)
val OnBackground = Color(0xFF0B0E14)
val OnSurface = Color(0xFF0B0E14)
val OnSurfaceVariant = Color(0xFF6B7280)
val OnError = Color.White

val Outline = Color(0xFFD6DAE3)
val OutlineVariant = Color(0xFFC2C8D3)

// Dark mode (the “glass + calm coach” feel)
val PrimaryDarkTheme = PrimaryMain
val SecondaryDarkTheme = SecondaryMain
val TertiaryDarkTheme = TertiaryMain

val BackgroundDark = Color(0xFF0B0E14)
val SurfaceDark = Color(0xFF0B0E14)
// Closest mapping: reference `--color-surface-container` (#161a21)
val SurfaceVariantDark = Color(0xFF161A21)

// Primary CTAs use white text on violet gradient (`redesigns/dashboard_violet_accent`)
val OnPrimaryDark = Color.White
val OnSecondaryDark = Color(0xFF0B0E14)
val OnTertiaryDark = Color(0xFF0B0E14)
val OnBackgroundDark = Color(0xFFECEDF6)
val OnSurfaceDark = Color(0xFFECEDF6)
val OnSurfaceVariantDark = Color(0xFFA9ABB3)

// Reference `--color-outline-variant` (#45484f)
val OutlineDark = Color(0xFF45484F)
val OutlineVariantDark = Color(0xFF45484F)

// Convenience semantics for later component refactors.
val Focus = PrimaryDarkTheme
val Block = Error

/** End stop for primary pill gradients (135°), matches `primary-gradient` in activities redesign */
val PrimaryGradientEnd = Color(0xFF7C3AED) // violet-600

/** Surfaces from redesigns folder HTML Tailwind tokens */
val SurfaceContainerLowestDark = Color(0xFF000000)
val SurfaceContainerLowDark = Color(0xFF10131A)
val SurfaceContainerDark = Color(0xFF161A21)
val SurfaceContainerHighDark = Color(0xFF1C2028)
val SurfaceContainerHighestDark = Color(0xFF22262F)
val SurfaceBrightDark = Color(0xFF282C36)
