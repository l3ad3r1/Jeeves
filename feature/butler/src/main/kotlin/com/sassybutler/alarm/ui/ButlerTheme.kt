package com.sassybutler.alarm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jeeves.core.theme.HermesTypography
import com.jeeves.core.theme.jeevesColorScheme

@Composable
fun ButlerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = jeevesColorScheme(darkTheme)
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HermesTypography,
        content = content
    )
}
