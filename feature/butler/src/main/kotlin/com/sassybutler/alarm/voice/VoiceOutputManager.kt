package com.sassybutler.alarm.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's [TextToSpeech] engine as a platform fallback for Butler's ONNX TTS.
 */
@Singleton
class VoiceOutputManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready: Boolean = false

    /** True if a TTS engine is available on this device. */
    fun isAvailable(): Boolean = tts != null && ready

    /**
     * Initialize the TTS engine. Safe to call multiple times.
     * Returns true once the engine reports [TextToSpeech.SUCCESS].
     */
    fun initialize(onReady: ((Boolean) -> Unit)? = null) {
        if (tts != null) {
            onReady?.invoke(ready)
            return
        }
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
                Log.i("VoiceOutput", "TTS engine ready")
            } else {
                Log.w("VoiceOutput", "TTS init failed: status=$status")
            }
            onReady?.invoke(ready)
        }
    }

    /**
     * Speak the given text. Returns a Flow that emits [VoiceOutputEvent.Start]
     * immediately, [VoiceOutputEvent.Done] when the engine finishes, or
     * [VoiceOutputEvent.Error] on failure.
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()): Flow<VoiceOutputEvent> = callbackFlow {
        val engine = tts
        if (engine == null || !ready) {
            trySend(VoiceOutputEvent.Error("TTS engine not ready"))
            awaitClose { }
            return@callbackFlow
        }
        if (text.isBlank()) {
            trySend(VoiceOutputEvent.Done)
            awaitClose { }
            return@callbackFlow
        }

        trySend(VoiceOutputEvent.Start)

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                trySend(VoiceOutputEvent.Done)
                channel.close()
            }
            override fun onError(id: String?) {
                trySend(VoiceOutputEvent.Error("TTS playback failed"))
                channel.close()
            }
        })

        engine.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

        awaitClose { }
    }

    /** Stop any in-progress speech. */
    fun stop() {
        tts?.stop()
    }

    /** Release native resources. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

sealed class VoiceOutputEvent {
    object Start : VoiceOutputEvent()
    object Done : VoiceOutputEvent()
    data class Error(val message: String) : VoiceOutputEvent()
}
