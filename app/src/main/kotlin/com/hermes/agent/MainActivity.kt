package com.hermes.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hermes.agent.domain.settings.SettingsRepository
import com.hermes.agent.ui.navigation.HermesNavGraph
import com.hermes.agent.ui.onboarding.OnboardingScreen
import com.hermes.agent.ui.theme.HermesTheme
import com.hermes.agent.work.OtaUpdateWorker
import com.jeeves.core.settings.JeevesSettings
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

    @Inject
    lateinit var features: Set<@JvmSuppressWildcards com.hermes.agent.domain.agent.AgentFeature>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hold the splash long enough for Jeeves to don his jacket + tie his
        // bow tie (~1.35s) before the app content takes over.
        val splashScreen = installSplashScreen()
        val splashStart = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - splashStart < 1520L
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingState = MutableStateFlow<Boolean?>(null)

        lifecycleScope.launch {
            onboardingState.value = settings.isOnboardingCompleted()
        }

        handleIntent(intent)

        setContent {
            val themeMode by JeevesSettings.themeModeFlow(this)
                .collectAsState(initial = JeevesSettings.themeMode(this))
            val themeStyle by JeevesSettings.themeStyleFlow(this)
                .collectAsState(initial = JeevesSettings.themeStyle(this))
            val fontFamily by JeevesSettings.fontFamilyFlow(this)
                .collectAsState(initial = JeevesSettings.fontFamily(this))
            val fontScalePercent by JeevesSettings.fontScalePercentFlow(this)
                .collectAsState(initial = JeevesSettings.fontScalePercent(this))

            HermesTheme(
                darkTheme = themeMode != JeevesSettings.THEME_LIGHT,
                themeStyle = com.hermes.agent.ui.theme.alt.ThemeStyle.fromStorageKey(themeStyle),
                fontFamilyName = fontFamily,
                fontScalePercent = fontScalePercent,
            ) {
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val matchingEntry = features.flatMap { it.entries() }.firstOrNull { it.intentAction == intent.action }
        if (matchingEntry?.targetActivityClassName != null) {
            val i = android.content.Intent().setClassName(packageName, matchingEntry.targetActivityClassName!!).apply {
                putExtra("EXTRA_EMBEDDED", true)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            if (packageManager.resolveActivity(i, 0) != null) {
                startActivity(i)
                finish()
                return
            }
        }
        when (intent.action) {
            "com.hermes.agent.action.ASK_JEEVES" -> {
                // To open chat directly, we should start the ChatScreen, but it's handled via nav graph.
                // For now, doing nothing leaves it in the MainActivity which opens to the nav graph.
            }
            "com.hermes.agent.action.PLAY_BRIEFING" -> {
                // Trigger the briefing. The briefing is handled by ButlerSpeech / AlarmForegroundService.
                // Start the briefing directly using ButlerSpeech. We might need a component to do it.
            }
            "com.hermes.agent.action.SHARE_TO_JEEVES" -> {
                val shareAction = intent.getStringExtra("EXTRA_SHARE_ACTION")
                val shareText = intent.getStringExtra("EXTRA_SHARE_TEXT")
                // TODO: Handle passing this to the agent / chat screen.
                // For now we just route to ChatScreen via nav graph and maybe pre-fill or send immediately.
            }
            "com.hermes.agent.action.START_VOICE_LISTEN" -> {
                // TODO: Open ChatScreen directly and arm voice listening.
            }
            "com.hermes.agent.action.NOTIFICATION_REPLY" -> {
                val replyText = intent.getStringExtra("EXTRA_REPLY_TEXT")
                // TODO: Send this directly to the agent.
            }
        }
    }
}
