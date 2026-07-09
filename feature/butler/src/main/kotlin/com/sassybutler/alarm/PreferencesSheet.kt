package com.sassybutler.alarm

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sassybutler.alarm.databinding.RowToggleBinding
import com.sassybutler.alarm.databinding.SheetPreferencesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Refining the Service" — butler preferences bottom sheet
 * (design: PreferencesSheet.tsx).
 *
 * Honorific · sass level · voice + preview · snooze duration · toggles.
 */
class PreferencesSheet(private val onChanged: () -> Unit = {}) : BottomSheetDialogFragment() {

    private var _binding: SheetPreferencesBinding? = null
    private val binding get() = _binding!!

    // Lazily created for voice preview only; the alarm service owns its own.
    private var previewEngine: TtsEngine? = null
    @Volatile private var previewTrack: AudioTrack? = null

    private val honorificChips = mutableMapOf<String, TextView>()
    private val snoozeChips = mutableMapOf<Int, TextView>()

    override fun getTheme() = R.style.ParlourBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = SheetPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupHonorifics()
        setupSass()
        setupVoicePicker()
        setupSnoozeChips()
        setupToggles()
        refreshQuips()
    }

    override fun onDestroyView() {
        previewTrack?.runCatching { stop(); release() }
        previewTrack = null
        previewEngine?.close()
        previewEngine = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onChanged()
    }

    // ─── Honorific ───────────────────────────────────────────────────────

    private fun setupHonorifics() {
        listOf("Sir", "Madam", "Boss").forEach { hon ->
            val chip = TextView(requireContext()).apply {
                text = hon
                gravity = Gravity.CENTER
                textSize = 16f
                // Downloadable font: getFont throws when not yet fetched —
                // fall back to the default face rather than crash.
                runCatching {
                    androidx.core.content.res.ResourcesCompat.getFont(
                        requireContext(), R.font.playfair_display)
                }.getOrNull()?.let { typeface = it }
                setTextColor(requireContext().getColorStateList(R.color.chip_text))
                background = requireContext().getDrawable(R.drawable.chip_bg)
                isSelected = ButlerPrefs.honorific(requireContext()) == hon
                setPadding(0, dp(14), 0, dp(14))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = dp(10) }
                setOnClickListener {
                    ButlerPrefs.setHonorific(requireContext(), hon)
                    honorificChips.values.forEach { it.isSelected = false }
                    isSelected = true
                    refreshQuips()
                }
            }
            honorificChips[hon] = chip
            binding.honorificChips.addView(chip)
        }
    }

    // ─── Sass level ──────────────────────────────────────────────────────

    private fun setupSass() {
        val initial = ButlerPrefs.sassLevel(requireContext())
        binding.seekSass.progress = initial
        updateSassLabels(initial)

        // Track width is 0 until the first layout pass; draw once it's known.
        binding.seekSass.post { drawSassGradient(initial) }

        binding.seekSass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    ButlerPrefs.setSassLevel(requireContext(), value)
                    updateSassLabels(value)
                }
                drawSassGradient(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })
    }

    /**
     * Paints the track so the green→blue transition always spans exactly
     * the filled portion (design: `linear-gradient(to right, green 0%,
     * blue pct%, grey pct%)`), rather than a fixed 0–100 gradient that only
     * shows a hint of blue near the high end. Regenerated on every change
     * since Android has no live-recalculated CSS-gradient equivalent.
     */
    private fun drawSassGradient(progress: Int) {
        val seekBar = _binding?.seekSass ?: return
        val w = seekBar.width
        if (w <= 0) return
        val h = dp(6)
        val radius = h / 2f
        val fillWidth = (w * progress / 100f).coerceAtLeast(1f)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val ctx = requireContext()

        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ctx.getColor(R.color.parchment) })

        if (progress > 0) {
            canvas.save()
            canvas.clipRect(0f, 0f, fillWidth, h.toFloat())
            canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, fillWidth, 0f,
                        ctx.getColor(R.color.apple_green), ctx.getColor(R.color.powder_blue),
                        Shader.TileMode.CLAMP)
                })
            canvas.restore()
        }
        seekBar.progressDrawable = BitmapDrawable(resources, bmp)
    }

    private fun updateSassLabels(level: Int) {
        binding.tvSassLabel.text = ButlerScript.sassTierName(level)
        binding.tvSassLabel.setTextColor(requireContext().getColor(
            if (level > 60) R.color.apple_green else R.color.powder_blue))
        binding.tvSassSample.text = "\"${sassSample(level)}\""
    }

    private fun sassSample(level: Int): String = when {
        level < 20 -> "Right away, naturally. A pleasure, as always."
        level < 40 -> "Of course. One endeavors to assist."
        level < 60 -> "I've woken you. The rest, I'm afraid, is up to you."
        level < 80 -> "Your alarm persists. Unlike, apparently, your resolve."
        else       -> "I have a PhD. This was not in the contract."
    }

    // ─── Voice ───────────────────────────────────────────────────────────

    private fun setupVoicePicker() {
        val ctx = requireContext()
        val adapter = ArrayAdapter(ctx, R.layout.item_voice, VoiceCatalog.VOICES.map { it.label })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVoice.adapter = adapter
        // setSelection(pos, false) applies synchronously, so the layout pass
        // doesn't fire a spurious onItemSelected that would clobber the pref.
        binding.spinnerVoice.setSelection(VoiceCatalog.indexOf(VoiceCatalog.selected(ctx)), false)

        binding.spinnerVoice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val name = VoiceCatalog.VOICES[position].name
                if (name != VoiceCatalog.selected(ctx)) VoiceCatalog.select(ctx, name)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.btnPreviewVoice.setOnClickListener { previewVoice() }
    }

    private fun previewVoice() {
        binding.btnPreviewVoice.isEnabled = false
        binding.btnPreviewVoice.text = "SYNTHESIZING…"
        val ctx = requireContext().applicationContext
        val voice = VoiceCatalog.selected(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            val engine = previewEngine ?: TtsEngine(ctx).also { previewEngine = it }
            val pcm = engine.synthesize(PREVIEW_LINE, voice)

            withContext(Dispatchers.Main) {
                _binding?.btnPreviewVoice?.text = "▶ PREVIEW"
                _binding?.btnPreviewVoice?.isEnabled = true
                if (pcm == null) {
                    Toast.makeText(ctx, "The butler has lost his voice. (TTS model missing?)",
                        Toast.LENGTH_SHORT).show()
                }
            }
            if (pcm != null) playPreview(pcm)
        }
    }

    /** Blocking Float32 PCM playback — call from Dispatchers.IO. */
    private fun playPreview(samples: FloatArray) {
        previewTrack?.runCatching { stop(); release() }

        val minBuf = AudioTrack.getMinBufferSize(
            TtsEngine.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(4096) * 4

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(TtsEngine.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        previewTrack = track
        track.play()
        var offset = 0
        while (offset < samples.size) {
            val wrote = track.write(samples, offset,
                minOf(minBuf / 4, samples.size - offset), AudioTrack.WRITE_BLOCKING)
            if (wrote < 0) break
            offset += wrote
        }
        runCatching { track.stop() } // MODE_STREAM: plays out the buffered tail
    }

    // ─── Snooze duration ─────────────────────────────────────────────────

    private fun setupSnoozeChips() {
        listOf(5, 10, 15, 20).forEach { minutes ->
            val chip = TextView(requireContext()).apply {
                text = "$minutes min"
                gravity = Gravity.CENTER
                textSize = 13f
                letterSpacing = 0.02f
                applyCinzel()
                setTextColor(requireContext().getColorStateList(R.color.chip_text))
                background = requireContext().getDrawable(R.drawable.chip_bg)
                isSelected = ButlerPrefs.snoozeMinutes(requireContext()) == minutes
                setPadding(0, dp(11), 0, dp(11))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = dp(8) }
                setOnClickListener {
                    ButlerPrefs.setSnoozeMinutes(requireContext(), minutes)
                    snoozeChips.values.forEach { it.isSelected = false }
                    isSelected = true
                }
            }
            snoozeChips[minutes] = chip
            binding.snoozeChips.addView(chip)
        }
    }

    // ─── Toggles ─────────────────────────────────────────────────────────

    private fun setupToggles() {
        val ctx = requireContext()
        addToggle("Voice wake-up monologue", "The butler speaks upon alarm",
            ButlerPrefs.voiceEnabled(ctx)) { ButlerPrefs.setVoiceEnabled(ctx, it) }
        addToggle("Birdsong overture", "Birds chirp before the butler speaks",
            ButlerPrefs.birdsIntro(ctx)) { ButlerPrefs.setBirdsIntro(ctx, it) }
        addToggle("Haptic accompaniment", "A refined vibration pattern",
            ButlerPrefs.haptics(ctx)) { ButlerPrefs.setHaptics(ctx, it) }
        addToggle("Snooze commentary", "He will note your indecision",
            ButlerPrefs.snoozeCommentary(ctx)) { ButlerPrefs.setSnoozeCommentary(ctx, it) }
    }

    private fun addToggle(label: String, sub: String, initial: Boolean,
                          onChange: (Boolean) -> Unit) {
        val row = RowToggleBinding.inflate(layoutInflater, binding.toggleRows, true)
        row.tvToggleLabel.text = label
        row.tvToggleSub.text = sub
        row.switchToggle.isChecked = initial
        row.switchToggle.setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun refreshQuips() {
        val hon = ButlerPrefs.honorific(requireContext())
        binding.tvHonorificQuip.text = "\"As you wish, $hon. A fine choice.\""
        binding.tvFooterQuip.text = "\"Your preferences have been noted, $hon.\""
    }

    private fun TextView.applyCinzel() {
        typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.cinzel)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        // Uses only words already in the generated PhonemeEncoder lexicon.
        private const val PREVIEW_LINE =
            "Good morning. I am delighted you survived the night."
    }
}
