package com.l3ad3r1.octojotter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.jeeves.core.theme.HermesTypography

// Notes uses the shared Jeeves palette from :core:theme; its old private
// "Inkwell" light/dark schemes were removed with the rebrand.

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Brand-first by default: use the Octo Jotter palette rather than wallpaper colors.
  // Users can opt back into Material You via the "Use system colors" setting.
  dynamicColor: Boolean = false,
  // Supplied by an enabled theme plugin; when non-null it wins over the built-ins.
  overrideColorScheme: ColorScheme? = null,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      overrideColorScheme != null -> overrideColorScheme

      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      // The one Jeeves palette (:core:theme). Notes is a feature of Jeeves, not a
      // separate product, so it no longer carries its own "Inkwell" scheme.
      else -> com.jeeves.core.theme.jeevesColorScheme(darkTheme)
    }

  val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

  CompositionLocalProvider(LocalOctoStatusColors provides statusColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = HermesTypography,
      content = content
    )
  }
}
