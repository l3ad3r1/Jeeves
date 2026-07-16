package com.hermes.agent.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hermes.agent.data.butler.BriefingComposer
import com.hermes.agent.data.proactive.ProactiveNotifier
import com.hermes.agent.domain.proactive.ProactiveSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Daily digest ping (roadmap v0.12): reuses the briefing composer's
 * weather/calendar/todos context as the digest body and routes it through
 * the proactive gate, so consent, DND, quiet hours, and the annoyance
 * budget all apply. Opt-in only — [ProactiveSource.DIGEST] defaults off.
 */
@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val briefingComposer: BriefingComposer,
    private val proactiveNotifier: ProactiveNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val body = briefingComposer.composeContext(applicationContext)
            .trim()
            .ifBlank { "Nothing on the radar today." }
            .take(1_500)
        proactiveNotifier.post(ProactiveSource.DIGEST, "Your daily digest", body)
        Result.success()
    } catch (e: Exception) {
        Timber.tag("DailyDigest").w(e, "digest composition failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "proactive_daily_digest"
    }
}
