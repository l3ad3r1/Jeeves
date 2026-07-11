package com.sassybutler.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_LINE = "Good morning. I am delighted you survived the night."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var honorific by remember { mutableStateOf(ButlerPrefs.honorific(context)) }
    var sassLevel by remember { mutableStateOf(ButlerPrefs.sassLevel(context).toFloat()) }
    var voice by remember { mutableStateOf(VoiceCatalog.selected(context)) }
    var snoozeMins by remember { mutableStateOf(ButlerPrefs.snoozeMinutes(context)) }
    var isPreviewing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var previewEngine by remember { mutableStateOf<TtsEngine?>(null) }
    var previewTrack by remember { mutableStateOf<AudioTrack?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewTrack?.runCatching { stop(); release() }
            previewEngine?.close()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Refining the Service", style = MaterialTheme.typography.titleLarge)
            Text(
                "\"As you wish, $honorific. A fine choice.\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Honorific
            Text("Address me as", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                listOf("Sir", "Madam", "Boss").forEach { hon ->
                    val isSelected = honorific == hon
                    Button(
                        onClick = { 
                            honorific = hon
                            ButlerPrefs.setHonorific(context, hon)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(hon)
                    }
                }
            }

            // Sass Level
            Text("Sass Level", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                ButlerScript.sassTierName(sassLevel.toInt()), 
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = sassLevel,
                onValueChange = { sassLevel = it },
                onValueChangeFinished = { ButlerPrefs.setSassLevel(context, sassLevel.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                "\"${sassSample(sassLevel.toInt())}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Voice
            Text("Voice", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = VoiceCatalog.VOICES.find { it.name == voice }?.label ?: voice,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    VoiceCatalog.VOICES.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.label) },
                            onClick = {
                                voice = v.name
                                VoiceCatalog.select(context, v.name)
                                expanded = false
                                if (v.name != "bm_daniel" && !VoiceDownloader.isDownloaded(context)) {
                                    VoiceDownloader.downloadVoices(context) {
                                        Toast.makeText(context, "Voice ready!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Button(
                onClick = {
                    isPreviewing = true
                    coroutineScope.launch(Dispatchers.IO) {
                        val engine = previewEngine ?: TtsEngine(context).also { previewEngine = it }
                        val pcm = engine.synthesize(PREVIEW_LINE, voice)
                        withContext(Dispatchers.Main) {
                            if (pcm == null) {
                                Toast.makeText(context, "Jeeves has lost his voice. (TTS model missing?)", Toast.LENGTH_SHORT).show()
                            }
                            isPreviewing = false
                        }
                        if (pcm != null) {
                            previewTrack?.runCatching { stop(); release() }
                            val track = createAudioTrack()
                            previewTrack = track
                            track.play()
                            var offset = 0
                            val minBuf = track.bufferSizeInFrames * 4
                            while (offset < pcm.size) {
                                val wrote = track.write(pcm, offset, minOf(minBuf / 4, pcm.size - offset), AudioTrack.WRITE_BLOCKING)
                                if (wrote < 0) break
                                offset += wrote
                            }
                            runCatching { track.stop() }
                        }
                    }
                },
                enabled = !isPreviewing,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(if (isPreviewing) "SYNTHESIZING..." else "PREVIEW VOICE")
            }

            // Snooze Duration
            Text("Snooze Duration", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                listOf(5, 10, 15, 20).forEach { mins ->
                    val isSelected = snoozeMins == mins
                    Button(
                        onClick = { 
                            snoozeMins = mins
                            ButlerPrefs.setSnoozeMinutes(context, mins)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp).weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("$mins min")
                    }
                }
            }

            // Toggles
            Text("Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            ToggleRow("Voice wake-up monologue", "Jeeves speaks upon alarm", ButlerPrefs.voiceEnabled(context)) { ButlerPrefs.setVoiceEnabled(context, it) }
            ToggleRow("Birdsong overture", "Birds chirp before Jeeves speaks", ButlerPrefs.birdsIntro(context)) { ButlerPrefs.setBirdsIntro(context, it) }
            ToggleRow("Haptic accompaniment", "A refined vibration pattern", ButlerPrefs.haptics(context)) { ButlerPrefs.setHaptics(context, it) }
            ToggleRow("Snooze commentary", "He will note your indecision", ButlerPrefs.snoozeCommentary(context)) { ButlerPrefs.setSnoozeCommentary(context, it) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, initial: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { 
                checked = it
                onCheckedChange(it) 
            }
        )
    }
}

private fun sassSample(level: Int): String = when {
    level < 20 -> "Right away, naturally. A pleasure, as always."
    level < 40 -> "Of course. One endeavors to assist."
    level < 60 -> "I've woken you. The rest, I'm afraid, is up to you."
    level < 80 -> "Your alarm persists. Unlike, apparently, your resolve."
    else       -> "I have a PhD. This was not in the contract."
}

private fun createAudioTrack(): AudioTrack {
    val minBuf = AudioTrack.getMinBufferSize(
        TtsEngine.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
    ).coerceAtLeast(4096) * 4

    return AudioTrack.Builder()
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
}
