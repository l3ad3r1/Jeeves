package com.hermes.agent

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hermes.agent.work.MemoryConsolidationWorker
import com.hermes.agent.work.OtaUpdateWorker
import com.hermes.agent.work.SkillImprovementWorker
import com.hermes.agent.data.log.FileLogTree
import com.hermes.agent.data.log.LogManager
import com.hermes.agent.data.performance.MemoryPressureMonitor
import com.hermes.agent.debug.DebugScreenAwake
import com.hermes.agent.domain.agent.AgentFeature
import com.jeeves.core.settings.JeevesSettings
import com.hermes.agent.domain.repository.ExecutionPlanRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

/**
 * Hermes Application entry point.
 *
 * Phase 1 responsibilities:
 *   - Bootstrap Hilt.
 *   - Initialize Timber logging.
 *   - Configure WorkManager with the Hilt-aware WorkerFactory so
 *     [MemoryConsolidationWorker] can inject its dependencies.
 *   - Schedule the periodic memory-consolidation worker (charging + idle
 *     constraint, runs once per day — see Section 5.4 and Section 6.2 of
 *     the plan).
 */
@HiltAndroidApp
class HermesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var memoryPressureMonitor: MemoryPressureMonitor

    @Inject
    lateinit var logManager: LogManager

    @Inject
    lateinit var noteIndexerProvider: Provider<com.hermes.agent.data.rag.NoteIndexer>

    @Inject
    lateinit var executionPlanRepositoryProvider: Provider<ExecutionPlanRepository>

    @Inject
    lateinit var encryptedSettingsProvider:
        Provider<com.hermes.agent.data.security.EncryptedSettingsRepository>

    @Inject
    lateinit var restoredSecretsProvider:
        Provider<com.hermes.agent.data.backup.RestoredSecretsApplier>

    @Inject
    lateinit var features: Set<@JvmSuppressWildcards AgentFeature>

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        DebugScreenAwake.install(this)
        // Capture logs to a file (all build types) so the user can pull them
        // from Settings → Logs; keep the console DebugTree in debug builds.
        Timber.plant(FileLogTree(logManager))
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Constructing NoteIndexer also constructs Jotter's encrypted token store and
        // database. Keep that work off the application injection/startup path so an
        // unavailable optional integration cannot prevent the rest of Jeeves starting.
        applicationScope.launch {
            runCatching { noteIndexerProvider.get().start(applicationScope) }
                .onFailure { Timber.tag("NoteIndexer").w(it, "note indexing unavailable") }
        }
        // A settings file restored from another install carries secrets encrypted
        // under that install's keystore key. They can never be decrypted here, so
        // they are cleared rather than left to masquerade as configured keys.
        // Idempotent and cheap, so it runs every start instead of needing a flag
        // handshake with the restore path.
        applicationScope.launch {
            // Order matters: credentials staged by a restore are applied first,
            // then the sweep clears anything still unreadable. Reversed, the
            // sweep would run before the restore had a chance to supply the
            // working values.
            runCatching { restoredSecretsProvider.get().applyPending() }
                .onFailure { Timber.tag("RestoreSecrets").w(it, "restore apply unavailable") }
            runCatching { encryptedSettingsProvider.get().clearUnreadableSecrets() }
                .onFailure { Timber.tag("Settings").w(it, "secret sweep unavailable") }
        }
        applicationScope.launch {
            runCatching { executionPlanRepositoryProvider.get().reconcileInterruptedSteps() }
                .onSuccess { count ->
                    if (count > 0) Timber.tag("ExecutionPlan").i("blocked %d interrupted steps", count)
                }
                .onFailure { Timber.tag("ExecutionPlan").w(it, "plan reconciliation unavailable") }
        }

        // Phase 4: start memory pressure polling. If the App Startup
        // initializer already started it via Hilt EntryPoint, this is a
        // no-op; otherwise we start it now that Hilt is initialized.
        memoryPressureMonitor.start()
        warmUpSettingsAndNotifyFeatures()
        scheduleMemoryConsolidation()
        scheduleSkillImprovement()
        scheduleOtaUpdateCheck()
    }

    private fun warmUpSettingsAndNotifyFeatures() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { JeevesSettings.prefs(this@HermesApp) }
                .onFailure { Timber.tag("Migration").w(it, "settings store warm-up failed") }
            features.forEach { feature ->
                runCatching { feature.onAppCreate(this@HermesApp, this) }
                    .onFailure { Timber.tag("Feature").w(it, "feature ${feature.id} onAppCreate failed") }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun scheduleOtaUpdateCheck() {
        // JX-01 (docs/UX_AUDIT.md): the OTA checker targets the STANDALONE
        // Hermes-Agent-Android release channel — the wrong channel for this
        // applicationId; "updating" would install a second, separate app.
        // Cancel rather than merely skip: earlier Jeeves builds already enqueued
        // this unique work with ExistingPeriodicWorkPolicy.KEEP, so on updated
        // installs it would otherwise keep running daily forever.
        if (!BuildConfig.OTA_ENABLED) {
            WorkManager.getInstance(this).cancelUniqueWork(OtaUpdateWorker.UNIQUE_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<OtaUpdateWorker>(
            1, TimeUnit.DAYS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OtaUpdateWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleSkillImprovement() {
        val request = PeriodicWorkRequestBuilder<SkillImprovementWorker>(
            7, TimeUnit.DAYS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SkillImprovementWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleMemoryConsolidation() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val request = PeriodicWorkRequestBuilder<MemoryConsolidationWorker>(
            1, TimeUnit.DAYS,
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MemoryConsolidationWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
