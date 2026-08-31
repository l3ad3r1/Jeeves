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
import com.hermes.agent.domain.repository.SkillRepository
import com.hermes.agent.data.mcp.McpManager
import com.hermes.agent.data.plugin.ScriptPluginRepository
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
    lateinit var skillRepositoryProvider: Provider<SkillRepository>

    @Inject
    lateinit var scriptPluginRepositoryProvider: Provider<ScriptPluginRepository>

    @Inject
    lateinit var mcpManagerProvider: Provider<McpManager>

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
            // Nothing stages credentials for a later start any more: the backup
            // restore applies them inline, so this is just the sweep.
            runCatching { encryptedSettingsProvider.get().clearUnreadableSecrets() }
                .onFailure { Timber.tag("Settings").w(it, "secret sweep unavailable") }

            // The Gist backup is gone, but an install that used it still holds
            // the GitHub token it was given. Deleting the feature does not
            // delete the credential, so clear it once here. Idempotent.
            runCatching { encryptedSettingsProvider.get().purgeRetiredGistCredentials() }
                .onFailure { Timber.tag("Settings").w(it, "retired-credential purge failed") }
        }
        // MCP tools are cached in Room after their first sync, but nothing loads
        // them back into the ToolRegistry on a cold start, so a configured server
        // would go quiet until the user opened Settings again. Same failure mode
        // the skills and modules seeding above exists to prevent.
        applicationScope.launch {
            runCatching { mcpManagerProvider.get().loadAndRegisterCachedTools() }
                .onFailure { Timber.tag("Mcp").w(it, "cached MCP tool registration failed") }
        }

        applicationScope.launch {
            runCatching { executionPlanRepositoryProvider.get().reconcileInterruptedSteps() }
                .onSuccess { count ->
                    if (count > 0) Timber.tag("ExecutionPlan").i("blocked %d interrupted steps", count)
                }
                .onFailure { Timber.tag("ExecutionPlan").w(it, "plan reconciliation unavailable") }
        }

        applicationScope.launch {
            runCatching { skillRepositoryProvider.get().seedBuiltIn() }
                .onFailure { Timber.tag("Skills").w(it, "built-in skill seeding failed") }
        }

        // Installed modules register their tools at startup. Without this the
        // agent would only see them after the user opened Settings → Modules,
        // so an installed module would silently do nothing until then.
        applicationScope.launch {
            runCatching { scriptPluginRepositoryProvider.get().reloadEnabled() }
                .onSuccess { failures ->
                    if (failures.isNotEmpty()) {
                        Timber.tag("Modules").w("modules failed to load: %s", failures.joinToString())
                    }
                }
                .onFailure { Timber.tag("Modules").w(it, "module loading unavailable") }
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
