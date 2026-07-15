package com.l3ad3r1.octojotter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jeeves.core.theme.jeevesColorScheme
import com.jeeves.core.theme.jeevesTypography

/** Notes shares Jeeves' monochrome mode and typography; color plugins stay disabled. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  overrideColorScheme: ColorScheme? = null,
  fontFamilyName: String = "geist",
  fontScalePercent: Int = 100,
  content: @Composable () -> Unit,
) {
  val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
  CompositionLocalProvider(LocalOctoStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = jeevesColorScheme(darkTheme),
      typography = jeevesTypography(fontFamilyName, fontScalePercent),
      content = content,
    )
  }
}
