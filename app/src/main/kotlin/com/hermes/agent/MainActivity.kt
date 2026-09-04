package com.hermes.agent

import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hermes.agent.domain.security.DeviceAuthenticationService
import com.hermes.agent.domain.settings.SettingsRepository
import com.hermes.agent.ui.chat.PendingChatIntent
import com.hermes.agent.ui.navigation.HermesNavGraph
import com.hermes.agent.ui.onboarding.OnboardingScreen
import com.hermes.agent.ui.theme.HermesTheme
import com.hermes.agent.work.OtaUpdateWorker
import com.jeeves.core.settings.JeevesSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Single-activity entry point. The Compose nav graph owns the screen
 * hierarchy — see [HermesNavGraph].
 *
 * Phase 4: shows the onboarding flow on first launch, then the main
 * nav graph on subsequent launches.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var deviceAuthenticationService: DeviceAuthenticationService

    @Inject
    lateinit var features: Set<@JvmSuppressWildcards com.hermes.agent.domain.agent.AgentFeature>

    /** Set by [handleIntent] on cold start (onCreate) or a re-delivered intent (onNewIntent). */
    private var pendingChatIntentTrigger by mutableStateOf(false)

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
        installDeviceAuthenticationHost()

        setContent {
            val themeMode by JeevesSettings.themeModeFlow(this)
                .collectAsState(initial = JeevesSettings.themeMode(this))
            val themeStyle by JeevesSettings.themeStyleFlow(this)
                .collectAsState(initial = JeevesSettings.themeStyle(this))
            val themeAccentColor by JeevesSettings.themeAccentColorFlow(this)
                .collectAsState(initial = JeevesSettings.themeAccentColor(this))
            val fontFamily by JeevesSettings.fontFamilyFlow(this)
                .collectAsState(initial = JeevesSettings.fontFamily(this))
            val fontScalePercent by JeevesSettings.fontScalePercentFlow(this)
                .collectAsState(initial = JeevesSettings.fontScalePercent(this))

            HermesTheme(
                // 'System' has to actually follow the system. Testing only against
                // THEME_LIGHT made THEME_SYSTEM -- the default -- resolve to dark
                // forever, so the three-way setting only ever offered two.
                darkTheme = when (themeMode) {
                    JeevesSettings.THEME_LIGHT -> false
                    JeevesSettings.THEME_DARK -> true
                    else -> isSystemInDarkTheme()
                },
                themeStyle = com.hermes.agent.ui.theme.alt.ThemeStyle.fromStorageKey(themeStyle),
                themeAccentColor = themeAccentColor,
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
                            startPendingChatIntent = pendingChatIntentTrigger,
                            onPendingChatIntentConsumed = { pendingChatIntentTrigger = false },
                        )
                    }
                }
            }
        }
    }

    /**
     * Shows the system biometric/device-credential prompt for whatever asked
     * [DeviceAuthenticationService] for confirmation.
     *
     * The service only *publishes* a request and then waits on a deferred; some
     * foreground Activity has to observe it, raise the prompt and submit the
     * answer. Without this, every authenticate() call blocks for the full 60 s
     * timeout and then resolves false -- which reads as a settings toggle that
     * silently refuses to flip, and as confirmation-gated tools that quietly
     * decline. Keep this in sync with the Hermes MainActivity host.
     */
    private fun installDeviceAuthenticationHost() {
        var activeRequestId: String? = null
        val prompt = BiometricPrompt(
            this,
            androidx.core.content.ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    activeRequestId?.let { deviceAuthenticationService.submit(it, true) }
                    activeRequestId = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    activeRequestId?.let { deviceAuthenticationService.submit(it, false) }
                    activeRequestId = null
                }
            },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceAuthenticationService.pendingRequest.collect { request ->
                    if (request == null) {
                        if (activeRequestId != null) prompt.cancelAuthentication()
                        activeRequestId = null
                        return@collect
                    }
                    if (request.id == activeRequestId) return@collect
                    activeRequestId = request.id
                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle(request.title)
                            .setSubtitle(request.reason)
                            .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                            )
                            .build(),
                    )
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
                // Opens to the nav graph's home screen — nothing further to route.
            }
            "com.hermes.agent.action.PLAY_BRIEFING" -> {
                // Still a stub: the daily voice briefing is owned by ButlerSpeech /
                // AlarmForegroundService (feature:butler), which :app has no direct
                // handle to from here today. Out of scope for the chat-intent fix
                // below — flagged separately, not addressed in this pass.
            }
            "com.hermes.agent.action.SHARE_TO_JEEVES" -> {
                // EXTRA_SHARE_ACTION (e.g. "summarize", "explain") is not yet used to
                // pick a persona/prompt template — the shared text is sent as-is.
                val shareText = intent.getStringExtra("EXTRA_SHARE_TEXT")
                if (!shareText.isNullOrBlank()) {
                    PendingChatIntent.publish(PendingChatIntent.Action.SendText(shareText))
                    pendingChatIntentTrigger = true
                }
            }
            "com.hermes.agent.action.START_VOICE_LISTEN" -> {
                PendingChatIntent.publish(PendingChatIntent.Action.ArmVoiceListen)
                pendingChatIntentTrigger = true
            }
            "com.hermes.agent.action.NOTIFICATION_REPLY" -> {
                val replyText = intent.getStringExtra("EXTRA_REPLY_TEXT")
                if (!replyText.isNullOrBlank()) {
                    PendingChatIntent.publish(PendingChatIntent.Action.SendText(replyText))
                    pendingChatIntentTrigger = true
                }
            }
        }
    }
}
