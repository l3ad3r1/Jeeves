package com.sassybutler.alarm

import android.content.Context
import android.util.Log
import ai.onnxruntime.*
import java.io.BufferedInputStream
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.zip.ZipInputStream

/**
 * TtsEngine — Kokoro ONNX Runtime inference wrapper.
 *
 * Model: Kokoro v1.0 int8 (thewh1teagle/kokoro-onnx release files)
 *   ─ app/src/main/assets/tts/kokoro-v1.0.int8.onnx
 *   ─ app/src/main/assets/tts/voices-v1.0.bin
 *
 * ONNX inputs (verified against the actual model):
 *   • "tokens"       — int64 [1, seq_len]   phoneme ID sequence
 *   • "style"        — float32 [1, 256]     voice style vector
 *   • "speed"        — float32 [1]          playback speed (1.0 = normal)
 *
 * ONNX output:
 *   • "audio"        — float32 [samples]    raw PCM waveform at 24 kHz
 *
 * ── voices-v1.0.bin format ──────────────────────────────────────────────
 *   A NumPy .npz archive (ZIP of "<voice>.npy" entries, 54 voices).
 *   Each entry: float32 little-endian, C-order, shape [510, 1, 256].
 *   The style vector fed to the model is voice[tokenCount] — Kokoro keys
 *   the style row on the *unpadded* phoneme count of the input.
 *
 * ── Phonemization note ─────────────────────────────────────────────────
 *   Full phonemization requires espeak-ng. This implementation uses the
 *   bundled [PhonemeEncoder] with a built-in US English phoneme→ID map.
 *   For production quality, integrate the espeak-ng NDK library.
 */
class TtsEngine(private val context: Context) : Closeable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    // Voice style tables loaded on demand from the .npz: name → [510][256]
    private val voiceCache = mutableMapOf<String, Array<FloatArray>>()

    private val phonemeEncoder = PhonemeEncoder()

    init {
        try {
            initSession()
            // Warm the user's voice so a missing/corrupt archive is
            // reported at init rather than mid-alarm.
            loadVoice(VoiceCatalog.selected(context))
        } catch (e: Exception) {
            Log.e(TAG, "TtsEngine init failed — TTS will be silent. Did you add the ONNX model?", e)
        }
    }

    // ─── Initialise ONNX session ─────────────────────────────────────────

    private fun initSession() {
        val modelBytes = context.assets.open(ASSET_MODEL).readBytes()

        val opts = OrtSession.SessionOptions().apply {
            // CPU execution provider only. NNAPI is deliberately NOT enabled:
            // it partitions this model into 100+ NNAPI subgraphs and crashes
            // natively (SIGFPE) inside createSession on some drivers, and the
            // API is deprecated (removed in Android 15). int8 CPU inference
            // is fast enough for these short greetings.
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }

        session = env.createSession(modelBytes, opts)

        // Log model I/O for debugging (remove in production)
        session?.inputInfo?.forEach { (name, info) ->
            Log.d(TAG, "Input  [$name]: ${info.info}")
        }
        session?.outputInfo?.forEach { (name, info) ->
            Log.d(TAG, "Output [$name]: ${info.info}")
        }
    }

    // ─── Voice loading (.npz parsing) ────────────────────────────────────

    /**
     * Load one voice's style table from the .npz archive.
     * Returns [510][256] rows, or null if the voice/archive is unusable.
     */
    private fun loadVoice(name: String): Array<FloatArray>? {
        voiceCache[name]?.let { return it }

        try {
            val downloadedFile = java.io.File(context.filesDir, VoiceDownloader.DOWNLOADED_VOICES_FILE)
            val inputStream = if (downloadedFile.exists() && downloadedFile.length() > 0) {
                java.io.FileInputStream(downloadedFile)
            } else {
                context.assets.open(ASSET_VOICES)
            }

            inputStream.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "$name.npy") {
                            val table = parseNpyStyleTable(zip.readBytes())
                            if (table != null) {
                                voiceCache[name] = table
                                Log.d(TAG, "Loaded voice '$name' (${table.size} style rows)")
                            }
                            return table
                        }
                        entry = zip.nextEntry
                    }
                }
            }
            Log.w(TAG, "Voice '$name' not found in bundle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load voice '$name'", e)
        }
        return null
    }

    /**
     * Parse a NumPy v1.0 .npy blob: "\x93NUMPY" magic, version bytes,
     * uint16-LE header length, ASCII header dict, then raw data.
     * Expects float32 little-endian with row width [STYLE_DIM].
     */
    private fun parseNpyStyleTable(bytes: ByteArray): Array<FloatArray>? {
        if (bytes.size < 10 ||
            bytes[0] != 0x93.toByte() ||
            String(bytes, 1, 5, Charsets.US_ASCII) != "NUMPY"
        ) {
            Log.e(TAG, "Not a .npy blob")
            return null
        }

        val headerLen  = ((bytes[9].toInt() and 0xFF) shl 8) or (bytes[8].toInt() and 0xFF)
        val dataOffset = 10 + headerLen
        val header     = String(bytes, 10, headerLen, Charsets.US_ASCII)

        if (!header.contains("'<f4'")) {
            Log.e(TAG, "Unexpected .npy dtype (want <f4): $header")
            return null
        }

        val floats = ByteBuffer.wrap(bytes, dataOffset, bytes.size - dataOffset)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()

        val rows = floats.remaining() / STYLE_DIM
        if (rows == 0) return null

        return Array(rows) { FloatArray(STYLE_DIM).also { floats.get(it) } }
    }

    // ─── Public: Synthesise ──────────────────────────────────────────────

    /**
     * Convert [text] to a Float32 PCM waveform at [SAMPLE_RATE] Hz.
     *
     * @param text        The English text to speak.
     * @param voiceName   Voice entry in voices-v1.0.bin (e.g. "bm_george").
     * @param speed       Playback speed multiplier (1.0 = normal).
     * @return            Float32 PCM samples, or null on failure.
     */
    fun synthesize(
        text: String,
        voiceName: String = DEFAULT_VOICE,
        speed: Float     = 1.0f
    ): FloatArray? {
        val sess = session ?: run {
            Log.w(TAG, "Session not initialised — returning silence")
            return null
        }

        // 1. Text → phoneme IDs ([0, ...tokens..., 0] — padded by the encoder)
        // Pronunciation dialect must match the voice family (en-gb vs en-us).
        var phonemeIds = phonemeEncoder.encode(text, VoiceCatalog.dialectFor(voiceName))
        if (phonemeIds.isEmpty()) {
            Log.w(TAG, "Empty phoneme sequence for: $text")
            return null
        }
        // The model (and the style table) supports at most MAX_TOKENS
        // unpadded tokens; truncate keeping the leading/trailing pads.
        if (phonemeIds.size > MAX_TOKENS + 2) {
            Log.w(TAG, "Token sequence ${phonemeIds.size - 2} > $MAX_TOKENS — truncating")
            phonemeIds = LongArray(MAX_TOKENS + 2).also { arr ->
                System.arraycopy(phonemeIds, 0, arr, 0, MAX_TOKENS + 1)
                arr[arr.size - 1] = 0L
            }
        }
        val tokenCount = phonemeIds.size - 2
        Log.d(TAG, "Phoneme IDs ($tokenCount): ${phonemeIds.take(20).joinToString()}")

        // 2. Style row — Kokoro indexes the voice table by unpadded token count
        val styleTable = loadVoice(voiceName) ?: loadVoice(DEFAULT_VOICE)
        val embedding  = styleTable?.let { it[tokenCount.coerceIn(0, it.size - 1)] }
            ?: FloatArray(STYLE_DIM) // silent fallback: neutral (zero) style

        // 3. Build input tensors
        val tokenTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(phonemeIds),
            longArrayOf(1L, phonemeIds.size.toLong())
        )
        val styleTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(embedding),
            longArrayOf(1L, STYLE_DIM.toLong())
        )
        val speedTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(floatArrayOf(speed)),
            longArrayOf(1L)
        )

        return try {
            val inputs = mapOf(
                "tokens" to tokenTensor,
                "style"  to styleTensor,
                "speed"  to speedTensor,
            )

            val results = sess.run(inputs)

            // output "audio": float32 [samples]
            val audioTensor = results[0] as OnnxTensor
            val rawData     = audioTensor.floatBuffer

            val numSamples = rawData.remaining()
            val pcm        = FloatArray(numSamples)
            rawData.get(pcm)

            Log.d(TAG, "Synthesized $numSamples samples (~${numSamples / SAMPLE_RATE}s)")
            pcm
        } catch (e: OrtException) {
            Log.e(TAG, "ONNX inference failed", e)
            null
        } finally {
            tokenTensor.close()
            styleTensor.close()
            speedTensor.close()
        }
    }

    // ─── Closeable ───────────────────────────────────────────────────────

    override fun close() {
        session?.close()
        session = null
        env.close()
    }

    // ─── Constants ───────────────────────────────────────────────────────

    companion object {
        private const val TAG          = "TtsEngine"
        private const val ASSET_MODEL  = "tts/kokoro-v1.0.int8.onnx"
        private const val ASSET_VOICES = "tts/voices-v1.0.bin"

        const val SAMPLE_RATE       = 24_000     // Hz — Kokoro v1 output rate
        // British male, per the butler persona. The PhonemeEncoder lexicon
        // is generated with espeak en-gb to match — keep dialect and voice
        // family in sync (tools/generate_phoneme_encoder.py).
        const val DEFAULT_VOICE     = "bm_george"
        private const val STYLE_DIM  = 256       // Kokoro style vector width
        private const val MAX_TOKENS = 510       // style table rows / model context
    }
}
