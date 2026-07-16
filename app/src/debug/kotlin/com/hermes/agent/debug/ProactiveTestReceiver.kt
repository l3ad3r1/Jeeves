package com.hermes.agent.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hermes.agent.data.proactive.BudgetStateStore
import com.hermes.agent.data.proactive.NotificationCaptureStore
import com.hermes.agent.data.proactive.ProactiveNotifier
import com.hermes.agent.domain.ledger.ActivityLedger
import com.hermes.agent.domain.proactive.ProactiveSource
import com.hermes.agent.domain.repository.MemoryRepository
import com.hermes.agent.work.CommitmentNudgeWorker
import com.hermes.agent.work.DailyDigestWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

/**
 * DEBUG-ONLY adb test seam for the proactive-engine device gates. Lives in
 * the debug source set — it does not exist in release builds. Every action
 * logs a `GATE:` line so the driver script can assert outcomes from logcat.
 *
 * Examples:
 *   adb shell am broadcast -a com.jeeves.debug.PROACTIVE_PING --es title T --es text X
 *   adb shell am broadcast -a com.jeeves.debug.SET_QUIET --ei start 10800 --ei end 10860
 *   adb shell am broadcast -a com.jeeves.debug.SET_CONSENT --es source DIGEST --ez granted true
 */
@AndroidEntryPoint
class ProactiveTestReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: ProactiveNotifier
    @Inject lateinit var store: BudgetStateStore
    @Inject lateinit var captureStore: NotificationCaptureStore
    @Inject lateinit var ledger: ActivityLedger
    @Inject lateinit var memoryRepository: MemoryRepository

    // Timber.tag() is one-shot and other code logs in between — re-tag per call.
    private fun gate(msg: String, vararg args: Any?) = Timber.tag("GATE").i(msg, *args)

    override fun onReceive(context: Context, intent: Intent) = runBlocking {
        when (intent.action) {
            "com.jeeves.debug.PROACTIVE_PING" -> {
                val source = sourceOf(intent)
                val posted = notifier.post(
                    source,
                    intent.getStringExtra("title") ?: "gate ping",
                    intent.getStringExtra("text") ?: "gate ping body",
                )
                gate("GATE:PING source=%s posted=%s", source.name, posted)
            }
            "com.jeeves.debug.RUN_DIGEST" -> {
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<DailyDigestWorker>().build())
                gate("GATE:DIGEST_ENQUEUED")
            }
            "com.jeeves.debug.RUN_NUDGE" -> {
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<CommitmentNudgeWorker>().build())
                gate("GATE:NUDGE_ENQUEUED")
            }
            "com.jeeves.debug.LESS_OF_THIS" -> {
                val source = sourceOf(intent)
                store.recordLessOfThis(source)
                gate("GATE:LESS source=%s count=%d", source.name, store.lessOfThisCount(source))
            }
            "com.jeeves.debug.RESET_LESS" -> {
                val source = sourceOf(intent)
                store.resetLessOfThis(source)
                gate("GATE:RESET_LESS source=%s", source.name)
            }
            "com.jeeves.debug.SET_CONSENT" -> {
                val source = sourceOf(intent)
                val granted = intent.getBooleanExtra("granted", false)
                store.setConsent(source, granted)
                gate("GATE:CONSENT source=%s granted=%s", source.name, granted)
            }
            "com.jeeves.debug.SET_QUIET" -> {
                val start = intent.getIntExtra("start", 22 * 3600)
                val end = intent.getIntExtra("end", 7 * 3600)
                store.setQuietHours(
                    LocalTime.ofSecondOfDay(start.toLong()),
                    LocalTime.ofSecondOfDay(end.toLong()),
                )
                gate("GATE:QUIET start=%s end=%s", store.quietStart, store.quietEnd)
            }
            "com.jeeves.debug.SET_CAPTURE" -> {
                captureStore.captureEnabled = intent.getBooleanExtra("enabled", false)
                if (!captureStore.captureEnabled) captureStore.clear()
                gate("GATE:CAPTURE enabled=%s", captureStore.captureEnabled)
            }
            "com.jeeves.debug.ADD_COMMITMENT" -> {
                val text = intent.getStringExtra("text") ?: "test the gates tonight"
                val id = memoryRepository.addMemory("Commitment: $text")
                gate("GATE:COMMITMENT id=%s", id)
            }
            "com.jeeves.debug.DUMP_LEDGER" -> {
                ledger.observeRecent(10).first().forEach { e ->
                    gate(
                        "GATE:LEDGER kind=%s title=%s success=%s detail=%s",
                        e.kind.name, e.title, e.success, e.detail.take(120),
                    )
                }
                gate("GATE:LEDGER_END")
            }
            else -> gate("GATE:UNKNOWN %s", intent.action)
        }
    }

    private fun sourceOf(intent: Intent): ProactiveSource =
        intent.getStringExtra("source")
            ?.let { name -> ProactiveSource.entries.firstOrNull { it.name == name } }
            ?: ProactiveSource.SCHEDULED_TASK
}
