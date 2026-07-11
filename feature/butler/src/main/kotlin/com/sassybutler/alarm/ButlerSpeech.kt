package com.sassybutler.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Butler's on-device ONNX voice, exposed as an injectable service so the Hermes agent can
 * speak its replies with it instead of the platform TextToSpeech engine.
 *
 * **Why this wrapper exists.** [TtsEngine]'s `init` block calls `initSession()`, which reads
 * the whole ~92 MB Kokoro model with `assets.open(...).readBytes()` and builds an ONNX Runtime
 * session *synchronously*. Constructing it from a Hilt `@Provides` would therefore load 92 MB
 * on whichever thread first injected it — quite possibly the main thread. So [TtsEngine] is
 * deliberately not a Hilt binding; this class owns it and builds it exactly once, lazily, on
 * [Dispatchers.IO], behind a [Mutex] so two concurrent `speak` calls cannot both load the model.
 *
 * Playback mirrors [AudioEngine]'s `playPcmBuffer` (Float32 PCM, no int16 conversion loss, tail
 * drained before release) with one deliberate difference: the audio attributes use
 * `USAGE_ASSISTANT` rather than `USAGE_ALARM`, so an agent reply is not routed as an alarm and
 * does not blast through Do Not Disturb or the alarm volume stream.
 *
 * All public functions are safe to call from any thread.
 */
@Singleton
class ButlerSpeech @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Outcome of [speak]. A plain Boolean conflated "the engine could not speak" with
     * "the user stopped it", and callers that fall back to another engine on failure
     * would then re-speak the very text the user just silenced.
     */
    enum class SpeakResult {
        /** Audio was synthesized and played to completion. */
        SPOKEN,

        /** [stop] (or cancellation) interrupted the utterance. Do NOT speak this text another way. */
        STOPPED,

        /** No audio could be produced (model missing, synthesis or playback failure). Falling back is appropriate. */
        UNAVAILABLE,
    }

    private val engineMutex = Mutex()

    @Volatile private var engine: TtsEngine? = null
    @Volatile private var track: AudioTrack? = null

    /** Set by [stop] to unblock an in-flight AudioTrack write and abort the drain loop. */
    private val stopped = AtomicBoolean(false)

    /** True once the model has been loaded successfully at least once. */
    val isLoaded: Boolean get() = engine != null

    /**
     * Build (or return) the ONNX engine. Loads ~92 MB on first call — always off the main
     * thread, and only once even under concurrent callers.
     *
     * Returns null if the model is missing or the session cannot be created; [TtsEngine]'s own
     * init swallows that and leaves `session` null, so [TtsEngine.synthesize] then yields null.
     */
    private suspend fun engine(): TtsEngine? {
        engine?.let { return it }
        return engineMutex.withLock {
            engine ?: withContext(Dispatchers.IO) {
                runCatching { TtsEngine(context) }
                    .onFailure { Log.e(TAG, "TtsEngine init failed — falling back to platform TTS", it) }
                    .getOrNull()
            }?.also { engine = it }
        }
    }

    /** Load the model ahead of time so the first spoken reply is not delayed. Safe to call twice. */
    suspend fun warmUp(): Boolean = engine() != null

    /**
     * Synthesize [text] and play it to completion. Suspends until playback finishes.
     *
     * @param voiceName null (the default) means "the voice the user picked in Butler's settings".
     *   It is resolved on [Dispatchers.IO], NOT as a Kotlin default argument: default arguments
     *   are evaluated at the call site, and `VoiceCatalog.selected()` reads SharedPreferences
     *   from disk — which would have run on the caller's thread, possibly the main thread.
     */
    suspend fun speak(
        text: String,
        voiceName: String? = null,
    ): SpeakResult {
        if (text.isBlank()) return SpeakResult.UNAVAILABLE
        stopped.set(false)

        return withContext(Dispatchers.IO) {
            val tts = engine() ?: return@withContext SpeakResult.UNAVAILABLE
            val voice = voiceName ?: VoiceCatalog.selected(context)

            // Split into sentences for streaming synthesis
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            var overallResult = SpeakResult.SPOKEN

            for (sentence in sentences) {
                if (sentence.isBlank()) continue
                if (stopped.get() || !currentCoroutineContext().isActive) {
                    overallResult = SpeakResult.STOPPED
                    break
                }

                val pcm = runCatching { tts.synthesize(sentence, voice) }
                    .onFailure { Log.e(TAG, "synthesis threw", it) }
                    .getOrNull()
                
                if (pcm == null || pcm.isEmpty()) {
                    Log.w(TAG, "synthesis produced no samples for sentence: $sentence")
                    continue
                }

                if (stopped.get() || !currentCoroutineContext().isActive) {
                    overallResult = SpeakResult.STOPPED
                    break
                }

                runCatching { playPcm(pcm) }
                    .onFailure { 
                        Log.e(TAG, "playback failed", it)
                        overallResult = SpeakResult.UNAVAILABLE
                    }
                
                if (overallResult == SpeakResult.UNAVAILABLE) break
            }

            overallResult
        }
    }

    /** Interrupt any in-flight playback. Idempotent. */
    fun stop() {
        stopped.set(true)
        track?.let { t ->
            runCatching {
                t.pause()   // unblocks a pending WRITE_BLOCKING
                t.flush()
                t.stop()
            }.onFailure { Log.w(TAG, "stop() on AudioTrack failed", it) }
        }
    }

    /**
     * Release the ONNX session and its ~92 MB of weights. Call under memory pressure; the next
     * [speak] transparently reloads. Not called automatically — the engine is kept warm because
     * reloading costs seconds.
     */
    suspend fun release() {
        stop()
        engineMutex.withLock {
            runCatching { engine?.close() }.onFailure { Log.w(TAG, "close() failed", it) }
            engine = null
        }
    }

    /** Mirrors AudioEngine.playPcmBuffer, but as assistant speech rather than an alarm. */
    private fun playPcm(samples: FloatArray) {
        val sampleRate = TtsEngine.SAMPLE_RATE
        val channelMask = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_FLOAT

        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
            .coerceAtLeast(4096) * 4 // float = 4 bytes

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // NOT USAGE_ALARM: an agent reply must not be routed to the alarm stream.
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track = t
        try {
            t.play()

            val chunkSize = minBufSize / 4 // floats, not bytes
            var offset = 0
            while (offset < samples.size && !stopped.get()) {
                val end = minOf(offset + chunkSize, samples.size)
                val wrote = t.write(samples, offset, end - offset, AudioTrack.WRITE_BLOCKING)
                if (wrote < 0) {
                    Log.e(TAG, "AudioTrack.write error: $wrote")
                    break
                }
                offset += wrote
            }

            // stop() in MODE_STREAM plays out what was already written; without draining, the
            // last ~bufferSize samples of speech are cut off.
            if (!stopped.get()) {
                runCatching {
                    t.stop()
                    var lastHead = -1
                    var stalledMs = 0
                    while (t.playbackHeadPosition < offset && !stopped.get() && stalledMs < 1_000) {
                        val head = t.playbackHeadPosition
                        stalledMs = if (head == lastHead) stalledMs + 20 else 0
                        lastHead = head
                        Thread.sleep(20)
                    }
                }.onFailure { Log.e(TAG, "Error draining AudioTrack", it) }
            }
        } finally {
            runCatching { t.release() }
            track = null
        }
        Log.d(TAG, "ButlerSpeech playback complete")
    }

    private companion object {
        const val TAG = "ButlerSpeech"
    }
}
