package com.jeeves.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The one Jeeves palette, shared by the agent surfaces, Notes and (via mirrored XML) Alarms.
 *
 * There is a single palette with a light and a dark variant, not three separate app themes.
 * What used to be Hermes' "Paper" and "Midnight" themes were always a light/dark pair of the
 * same monochrome design, so they became [JeevesLight] and [JeevesDark]; the user's Dark mode
 * setting picks between them. "Blue" survives as a genuinely different accent brand, and is
 * dark-only.
 *
 * Butler's Views cannot consume a Compose [ColorScheme]. Its XML themes mirror these hex
 * values in `feature/butler/src/main/res/values/colors.xml` (light) and `values-night/`
 * (dark). Change one, change the other.
 */
object JeevesPalette {

    // ── Dark (formerly "Midnight") — monochrome, pure black ──────────────
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF111111)
    val DarkSurfaceVariant = Color(0xFF1C1C1C)
    val DarkOnBackground = Color(0xFFFFFFFF)
    val DarkOnSurfaceVariant = Color(0xFF999999)
    val DarkOutline = Color(0xFF333333)
    val DarkPrimary = Color(0xFFFFFFFF)
    val DarkOnPrimary = Color(0xFF000000)
    val DarkPrimaryContainer = Color(0xFF222222)
    val DarkSecondary = Color(0xFFCCCCCC)
    val DarkSecondaryContainer = Color(0xFF1A1A1A)
    val DarkError = Color(0xFFFF4444)

    // ── Light (formerly "Paper") ─────────────────────────────────────────
    val LightBackground = Color(0xFFF3F2EE)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0EFEA)
    val LightOnBackground = Color(0xFF0D0D12)
    val LightOnSurfaceVariant = Color(0xFF5D5E6A)
    val LightOutline = Color(0xFF9596A1)
    val LightPrimary = Color(0xFF0000F2)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFE2E2FE)
    val LightSecondary = Color(0xFF1F9D63)
    val LightSecondaryContainer = Color(0xFFD7F0E2)
    val LightError = Color(0xFFCC0000)

    // ── Blue accent brand (dark-only) ────────────────────────────────────
    val BlueBackground = Color(0xFF3300FF)
    val BlueOnBackground = Color(0xFFFFFFFF)
}

/** Dark variant of the Jeeves palette. */
val JeevesDark: ColorScheme = darkColorScheme(
    primary = JeevesPalette.DarkPrimary,
    onPrimary = JeevesPalette.DarkOnPrimary,
    primaryContainer = JeevesPalette.DarkPrimaryContainer,
    onPrimaryContainer = JeevesPalette.DarkOnBackground,
    secondary = JeevesPalette.DarkSecondary,
    onSecondary = JeevesPalette.DarkOnPrimary,
    secondaryContainer = JeevesPalette.DarkSecondaryContainer,
    tertiary = Color(0xFFAAAAAA),
    onTertiary = Color(0xFF000000),
    background = JeevesPalette.DarkBackground,
    onBackground = JeevesPalette.DarkOnBackground,
    surface = JeevesPalette.DarkSurface,
    onSurface = JeevesPalette.DarkOnBackground,
    surfaceVariant = JeevesPalette.DarkSurfaceVariant,
    onSurfaceVariant = JeevesPalette.DarkOnSurfaceVariant,
    surfaceContainer = JeevesPalette.DarkSurface,
    surfaceContainerHigh = JeevesPalette.DarkSurfaceVariant,
    outline = JeevesPalette.DarkOutline,
    outlineVariant = JeevesPalette.DarkSurfaceVariant,
    error = JeevesPalette.DarkError,
    onError = Color.Black,
)

/** Light variant of the Jeeves palette. */
val JeevesLight: ColorScheme = lightColorScheme(
    primary = JeevesPalette.LightPrimary,
    onPrimary = JeevesPalette.LightOnPrimary,
    primaryContainer = JeevesPalette.LightPrimaryContainer,
    onPrimaryContainer = JeevesPalette.LightPrimary,
    secondary = JeevesPalette.LightSecondary,
    onSecondary = JeevesPalette.LightOnPrimary,
    secondaryContainer = JeevesPalette.LightSecondaryContainer,
    tertiary = JeevesPalette.LightSecondary,
    onTertiary = Color.White,
    background = JeevesPalette.LightBackground,
    onBackground = JeevesPalette.LightOnBackground,
    surface = JeevesPalette.LightSurface,
    onSurface = JeevesPalette.LightOnBackground,
    surfaceVariant = JeevesPalette.LightSurfaceVariant,
    onSurfaceVariant = JeevesPalette.LightOnSurfaceVariant,
    surfaceContainer = JeevesPalette.LightSurface,
    surfaceContainerHigh = JeevesPalette.LightSurfaceVariant,
    outline = JeevesPalette.LightOutline,
    outlineVariant = JeevesPalette.LightSurfaceVariant,
    error = JeevesPalette.LightError,
    onError = Color.White,
)

/**
 * Pick the Jeeves scheme for the current mode.
 * Callers pass the app-wide dark-mode setting, not `isSystemInDarkTheme()` directly.
 */
fun jeevesColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) JeevesDark else JeevesLight
