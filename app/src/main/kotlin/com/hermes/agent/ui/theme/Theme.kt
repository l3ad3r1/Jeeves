package com.hermes.agent.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.jeeves.core.theme.jeevesColorScheme
import com.jeeves.core.theme.HermesTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background

/**
 * The three Hermes app themes derived from the brand site (hermes-agent.nousresearch.com).
 *
 * MIDNIGHT — black background, white text. Terminal/hacker aesthetic.
 * PAPER    — white background, black text. Clean editorial aesthetic.
 * HERMES_BLUE — electric blue (#3300FF) background, white text. Matches the brand site exactly.
 */
enum class AppTheme {
    MIDNIGHT,
    PAPER,
    HERMES_BLUE,
}

// ── Hermes Blue color scheme ──────────────────────────────────────────
private val HermesBlueColorScheme = darkColorScheme(
    primary              = BlueBrandPrimary,
    onPrimary            = BlueBrandOnPrimary,
    primaryContainer     = BlueBrandPrimaryContainer,
    onPrimaryContainer   = BlueBrandOnPrimaryContainer,
    secondary            = BlueBrandSecondary,
    onSecondary          = BlueBrandOnSecondary,
    secondaryContainer   = BlueBrandSecondaryContainer,
    background           = BlueBrandBackground,
    onBackground         = BlueBrandOnBackground,
    surface              = BlueBrandSurface,
    onSurface            = BlueBrandOnSurface,
    surfaceVariant       = BlueBrandSurfaceVariant,
    onSurfaceVariant     = BlueBrandOnSurfaceVariant,
    error                = BlueBrandError,
    onError              = Color.White,
)

/**
 * Root theme composable — the one Jeeves theme, shared with Notes via `:core:theme`.
 *
 * [appTheme] selects the accent brand, [darkTheme] selects the light/dark variant. They are
 * orthogonal: MIDNIGHT and PAPER were always the dark and light halves of the same monochrome
 * palette, so both now map to the shared Jeeves scheme and the app-wide Dark mode setting
 * decides which half is used. HERMES_BLUE is a genuinely different brand and is dark-only.
 *
 * Previously a non-null [appTheme] short-circuited [darkTheme] entirely — and MainActivity
 * always passes one (defaulting to MIDNIGHT), so the Dark mode setting never affected this
 * app's own surfaces.
 */
@Composable
fun HermesTheme(
    appTheme: AppTheme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        // Dark-only accent brand.
        AppTheme.HERMES_BLUE -> HermesBlueColorScheme
        // MIDNIGHT / PAPER / null -> the shared palette; the mode picks the variant.
        else -> jeevesColorScheme(darkTheme)
    }

    val isDark = appTheme == AppTheme.HERMES_BLUE || darkTheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val context = LocalContext.current
    val fontScale = context.resources.configuration.fontScale
    val typography = if (fontScale > 1.2f) boostedTypography(HermesTypography) else HermesTypography

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = HermesShapes,
    ) {
        HermesHighContrastWrapper(darkTheme = darkTheme, content = content)
    }
}

/**
 * Premium glassmorphism modifier that applies a translucent background 
 * and blur effect (on supported API levels), creating depth.
 */
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

fun Modifier.glassBackground(
    color: Color,
    alpha: Float = 0.65f,
    blurRadius: Float = 16f
): Modifier = this
    .blur(blurRadius.dp)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.8f)
            )
        )
    )

/**
 * Premium gradient background modifier for primary buttons and accents.
 */
fun Modifier.premiumGradient(color: Color): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.85f)
        )
    )
)
