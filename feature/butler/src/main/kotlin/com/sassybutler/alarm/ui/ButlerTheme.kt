package com.sassybutler.alarm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.jeeves.core.settings.JeevesSettings
import com.jeeves.core.theme.jeevesColorScheme
import com.jeeves.core.theme.jeevesTypography

@Suppress("UNUSED_PARAMETER")
@Composable
fun ButlerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themeMode by JeevesSettings.themeModeFlow(context)
        .collectAsState(initial = JeevesSettings.themeMode(context))
    val fontFamily by JeevesSettings.fontFamilyFlow(context)
        .collectAsState(initial = JeevesSettings.fontFamily(context))
    val fontScalePercent by JeevesSettings.fontScalePercentFlow(context)
        .collectAsState(initial = JeevesSettings.fontScalePercent(context))
    val sharedDarkMode = themeMode != JeevesSettings.THEME_LIGHT

    MaterialTheme(
        colorScheme = jeevesColorScheme(sharedDarkMode),
        typography = jeevesTypography(fontFamily, fontScalePercent),
        content = content,
    )
}
