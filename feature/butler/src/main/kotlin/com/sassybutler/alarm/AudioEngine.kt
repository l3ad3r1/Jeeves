package com.sassybutler.alarm

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AudioEngine — Orchestrates the full alarm audio pipeline.
 *
 * Sequence:
 *  1. Play [R.raw.birds_chirping] via [MediaPlayer].
 *  2. While birds play, concurrently run ONNX TTS inference in background.
 *  3. When birds complete, stream generated PCM via [AudioTrack].
 *  4. On dismiss: stop current audio, synthesise a randomised sarcastic
 *     reaction, play it via AudioTrack.
 *
 * Thread safety: all public methods are safe to call from any thread.
 * The scope is tied to the service; call [release] in onDestroy.
 */
class AudioEngine(private val context: Context) {

    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ttsEngine  = TtsEngine(context)

    // Volatile: written by playback coroutines (IO), read by stopAll() from
    // the main thread — stale reads mean stopping an already-released track.
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var audioTrack: AudioTrack?   = null
    @Volatile private var fallbackRingtone: Ringtone? = null

    private val isStopped  = AtomicBoolean(false)
    private val isDismissed= AtomicBoolean(false)

    // ─── Public API ─────────────────────────────────────────────────────

    /**
     * Begin the full alarm sequence:
     *  • Start birds chirping immediately.
     *  • In parallel synthesize the greeting TTS.
     *  • Chain AudioTrack playback to birds completion.
     */
    suspend fun playAlarmSequence(
        greeting: String,
        skipBirds: Boolean = false,
        voiceEnabled: Boolean = true,
        onBirdsComplete: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {

        isStopped.set(false)

        if (!voiceEnabled) {
            // Master voice switch is off: there is no TTS to carry the
            // alarm, so loop the birdsong indefinitely as the wake sound.
            // If the birds player can't start, an alarm with no sound is
            // unacceptable — fall back to the system alarm ringtone.
            Log.d(TAG, "Voice disabled — looping birds as the wake sound")
            withContext(Dispatchers.Main) {
                startBirdsPlayer(loop = true, onError = { startFallbackRingtone() })
            }
            return@withContext
        }

        // --- Step 1: Kick off TTS synthesis in parallel ---------------------
        val ttsDeferred: Deferred<FloatArray?> = async {
            try {
                Log.d(TAG, "TTS synthesis started")
                ttsEngine.synthesize(greeting, VoiceCatalog.selected(context)).also {
                    Log.d(TAG, "TTS synthesis complete — ${it?.size ?: 0} samples")
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS synthesis failed", e)
                null
            }
        }

        // --- Step 2: Play birds chirping via MediaPlayer -------------------
        val birdsFinished = CompletableDeferred<Unit>()

        if (skipBirds) {
            Log.d(TAG, "Birds intro disabled — straight to the greeting")
            birdsFinished.complete(Unit)
        } else withContext(Dispatchers.Main) {
            startBirdsPlayer(
                loop = false,
                onComplete = { onBirdsComplete(); birdsFinished.complete(Unit) },
                onError = { birdsFinished.complete(Unit) }, // don't hang
            )
        }

        // --- Step 3: Wait for birds to finish ------------------------------
        birdsFinished.await()

        if (isStopped.get()) return@withContext

        // --- Step 4: Get TTS result and play via AudioTrack ----------------
        val pcmSamples = ttsDeferred.await()
        if (pcmSamples != null && !isStopped.get()) {
            playPcmBuffer(pcmSamples)
        }
    }

    /**
     * Start [R.raw.birds_chirping] on the main thread. Attributes must be
     * applied at create time: MediaPlayer.create() returns an already
     * prepared player, and setAudioAttributes() after prepare is invalid —
     * USAGE_ALARM would silently not apply.
     */
    private fun startBirdsPlayer(loop: Boolean, onComplete: () -> Unit = {}, onError: () -> Unit = {}) {
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val sessionId = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .generateAudioSessionId()

        val player = MediaPlayer.create(context, R.raw.birds_chirping, alarmAttributes, sessionId)
        if (player == null) {
            // create() returns null when the resource can't be decoded —
            // let the caller fall through rather than crash silent.
            Log.e(TAG, "MediaPlayer.create failed — skipping birds")
            onError()
            return
        }
        mediaPlayer = player.apply {
            isLooping = loop
            if (!loop) {
                setOnCompletionListener { onComplete() }
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                onError()
                true
            }
            start()
            Log.d(TAG, "MediaPlayer started (birds, loop=$loop)")
        }
    }

    /** Last-resort wake sound when the birds player itself fails. */
    private fun startFallbackRingtone() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            fallbackRingtone = RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
                Log.w(TAG, "Fallback system alarm ringtone playing")
            }
        }.onFailure { Log.e(TAG, "Fallback ringtone failed too", it) }
    }

    /**
     * Stop all audio immediately, synthesise a sarcastic dismissal line,
     * then play it back.
     */
    suspend fun dismissWithReaction() = withContext(Dispatchers.IO) {
        isDismissed.set(true)
        stopAll()
        // stopAll() latches isStopped to kill the alarm audio; clear it so the
        // reaction itself is allowed to play through playPcmBuffer.
        isStopped.set(false)

        val reaction = ButlerScript.dismissReaction()
        Log.i(TAG, "Dismiss reaction: $reaction")
        synthesizeAndPlay(reaction)
    }

    /**
     * Speak an arbitrary line (e.g. snooze commentary), interrupting any
     * current audio. Returns when playback finishes.
     */
    suspend fun speak(line: String) = withContext(Dispatchers.IO) {
        stopAll()
        isStopped.set(false)
        Log.i(TAG, "Speaking: $line")
        synthesizeAndPlay(line)
    }

    private fun synthesizeAndPlay(text: String) {
        val pcm = try {
            ttsEngine.synthesize(text, VoiceCatalog.selected(context))
        } catch (e: Exception) {
            Log.e(TAG, "TTS failed", e)
            null
        }
        if (pcm != null) playPcmBuffer(pcm)
    }

    /** Immediately halt all audio (MediaPlayer + AudioTrack + fallback). */
    fun stopAll() {
        isStopped.set(true)
        runCatching { fallbackRingtone?.stop() }
        fallbackRingtone = null
        try {
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            audioTrack?.run {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    pause()
                    flush()
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }

    fun release() {
        scope.cancel()
        stopAll()
        ttsEngine.close()
    }

    // ─── Private: AudioTrack PCM playback ───────────────────────────────

    /**
     * Stream a Float32 PCM buffer to [AudioTrack].
     * Uses [AudioFormat.ENCODING_PCM_FLOAT] which avoids int16 conversion loss.
     */
    private fun playPcmBuffer(samples: FloatArray) {
        if (isStopped.get()) return

        val sampleRate   = TtsEngine.SAMPLE_RATE
        val channelMask  = AudioFormat.CHANNEL_OUT_MONO
        val encoding     = AudioFormat.ENCODING_PCM_FLOAT

        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
            .coerceAtLeast(4096) * 4  // float = 4 bytes

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
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

        audioTrack = track
        track.play()

        // Write in chunks so we can check for stop signals between chunks.
        // Blocking writes are fine here (Dispatchers.IO) and avoid the hot
        // spin a non-blocking write causes while the track buffer is full;
        // stopAll()'s pause/flush unblocks a pending write on dismiss.
        val chunkSize = minBufSize / 4 // number of floats
        var offset    = 0
        while (offset < samples.size && !isStopped.get()) {
            val end   = minOf(offset + chunkSize, samples.size)
            val wrote = track.write(samples, offset, end - offset, AudioTrack.WRITE_BLOCKING)
            if (wrote < 0) {
                Log.e(TAG, "AudioTrack.write error: $wrote")
                break
            }
            offset += wrote
        }

        // Let the buffered tail play out before releasing, otherwise the last
        // ~bufferSize samples of speech are cut off. stop() in MODE_STREAM
        // plays out what was written; the head position tells us when it's done.
        if (!isStopped.get()) {
            try {
                track.stop()
                var lastHead = -1
                var stalledMs = 0
                while (track.playbackHeadPosition < offset && !isStopped.get() && stalledMs < 1_000) {
                    val head = track.playbackHeadPosition
                    stalledMs = if (head == lastHead) stalledMs + 20 else 0
                    lastHead = head
                    Thread.sleep(20)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error draining AudioTrack", e)
            }
        }
        track.release()
        audioTrack = null
        Log.d(TAG, "AudioTrack playback complete")
    }

    // ─── Constants ───────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AudioEngine"
    }
}
