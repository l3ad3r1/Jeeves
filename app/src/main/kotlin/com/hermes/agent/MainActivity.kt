package com.hermes.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hermes.agent.data.settings.SettingsRepository
import com.jeeves.core.settings.JeevesSettings
import com.hermes.agent.ui.navigation.HermesNavGraph
import com.hermes.agent.ui.onboarding.OnboardingScreen
import com.hermes.agent.ui.theme.AppTheme
import com.hermes.agent.ui.theme.HermesTheme
import com.hermes.agent.work.OtaUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity entry point. The Compose nav graph owns the screen
 * hierarchy — see [HermesNavGraph].
 *
 * Phase 4: shows the onboarding flow on first launch, then the main
 * nav graph on subsequent launches.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingState = MutableStateFlow<Boolean?>(null)

        lifecycleScope.launch {
            onboardingState.value = settings.isOnboardingCompleted()
        }

        setContent {
            val settingsState by settings.observe().collectAsState(initial = null)

            val appTheme = settingsState?.appTheme?.let { name ->
                runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.MIDNIGHT)
            }

            // App-wide light/dark/system, from the one settings store shared with Notes.
            val themeMode by JeevesSettings.themeModeFlow(this)
                .collectAsState(initial = JeevesSettings.themeMode(this))
            val darkTheme = when (themeMode) {
                JeevesSettings.THEME_DARK -> true
                JeevesSettings.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            HermesTheme(appTheme = appTheme, darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by onboardingState.collectAsState()
                    when (state) {
                        null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        false -> OnboardingScreen(
                            onCompleted = {
                                onboardingState.value = true
                            },
                        )
                        true -> HermesNavGraph(
                            // Update notification deep-links to Settings → Updates.
                            startAtSettings = intent?.getBooleanExtra(
                                OtaUpdateWorker.EXTRA_OPEN_UPDATES, false,
                            ) == true,
                        )
                    }
                }
            }
        }
    }
}
