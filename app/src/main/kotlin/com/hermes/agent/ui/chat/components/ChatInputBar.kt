package com.hermes.agent.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hermes.agent.R

/** Reasoning-effort levels, ordered least → most, as the slider steps through them. */
private val EFFORT_LEVELS = listOf("minimal", "low", "medium", "high")

/**
 * Trims a raw model id down for the composer's action row: `claude-sonnet-5` →
 * `Sonnet 5`, `gpt-4.1-mini` → `GPT 4.1 Mini`, blank → `Auto`.
 *
 * ponytail: cosmetic label — short tokens read as acronyms, the rest title-case;
 * good enough without a per-vendor lookup table.
 */
internal fun shortModelName(raw: String): String =
    raw.trim().substringAfterLast('/').removePrefix("claude-").removePrefix("anthropic-")
        .split('-', ' ').filter { it.isNotBlank() }
        .joinToString(" ") { if (it.length <= 3) it.uppercase() else it.replaceFirstChar(Char::uppercase) }
        .ifBlank { "Auto" }

/**
 * Rounded composer: a full-width text field on top, and a single action row
 * beneath it — attachments and microphone on the left, the shortened model name
 * and a reasoning-effort button (with a slider inside) on the right, then send.
 */
@Composable
fun ChatInputBar(
    isSending: Boolean,
    isListening: Boolean,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onMicToggle: () -> Unit,
    onVoiceChatToggle: () -> Unit,
    modifier: Modifier = Modifier,
    prefillText: String = "",
    onSendWithAttachment: ((String, String?, String?) -> Unit)? = null,
    reasoningEffort: String = "medium",
    onReasoningEffortChange: ((String) -> Unit)? = null,
    modelName: String = "",
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by remember(prefillText) { mutableStateOf(prefillText) }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var quickActionsOpen by remember { mutableStateOf(false) }
    var effortMenuOpen by remember { mutableStateOf(false) }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        attachedImageUri = uri
    }

    fun submit() {
        val message = text.trim()
        if (message.isNotEmpty() || attachedImageUri != null) {
            val uriStr = attachedImageUri?.toString()
            val mime = attachedImageUri?.let { context.contentResolver.getType(it) }
            if (onSendWithAttachment != null) {
                onSendWithAttachment(message, uriStr, mime)
            } else {
                onSend(message)
            }
            text = ""
            attachedImageUri = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        attachedImageUri?.let { uri ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Image attached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { attachedImageUri = null }
                        .padding(horizontal = 4.dp),
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 148.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    maxLines = 6,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.chat_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            onClick = { quickActionsOpen = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Attachments and actions")
                        }
                        DropdownMenu(
                            expanded = quickActionsOpen,
                            onDismissRequest = { quickActionsOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Attach image") },
                                onClick = {
                                    quickActionsOpen = false
                                    imagePickerLauncher.launch("image/*")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Attach document") },
                                onClick = {
                                    quickActionsOpen = false
                                    imagePickerLauncher.launch("*/*")
                                },
                            )
                            listOf(
                                "Plan my day" to "Help me plan my day",
                                "Create a note" to "Create a note for me",
                                "Look something up" to "Look something up for me",
                            ).forEach { (label, prompt) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        text = prompt
                                        quickActionsOpen = false
                                    },
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onMicToggle,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = stringResource(R.string.a11y_voice_input),
                            tint = if (isListening) {
                                MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = shortModelName(modelName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )

                    if (onReasoningEffortChange != null) {
                        Box {
                            TextButton(
                                onClick = { effortMenuOpen = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = reasoningEffort.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            DropdownMenu(
                                expanded = effortMenuOpen,
                                onDismissRequest = { effortMenuOpen = false },
                            ) {
                                val current = EFFORT_LEVELS.indexOf(reasoningEffort)
                                    .let { if (it < 0) EFFORT_LEVELS.indexOf("medium") else it }
                                Column(modifier = Modifier.width(248.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    // Doubles as the label; updates live while dragging.
                                    Text(
                                        text = "Reasoning effort: ${EFFORT_LEVELS[current].replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Slider(
                                        value = current.toFloat(),
                                        onValueChange = { v ->
                                            val idx = v.toInt().coerceIn(0, EFFORT_LEVELS.lastIndex)
                                            if (EFFORT_LEVELS[idx] != reasoningEffort) {
                                                onReasoningEffortChange(EFFORT_LEVELS[idx])
                                            }
                                        },
                                        valueRange = 0f..EFFORT_LEVELS.lastIndex.toFloat(),
                                        steps = EFFORT_LEVELS.size - 2,
                                    )
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Minimal",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "High",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.size(4.dp))

                    // One shape — a keyboard-return glyph — regardless of
                    // whether there is text; only the colour shifts.
                    val hasText = text.isNotBlank()
                    val actionColor = when {
                        isSending -> Color(0xFFE5484D) // red stop while Jeeves is replying
                        hasText -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Surface(
                        onClick = when {
                            isSending -> onCancel
                            hasText -> ::submit
                            else -> onMicToggle
                        },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = actionColor,
                        contentColor = when {
                            isSending -> Color.White
                            hasText -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSending) {
                                    Icons.Outlined.Stop
                                } else {
                                    Icons.AutoMirrored.Outlined.KeyboardReturn
                                },
                                contentDescription = when {
                                    isSending -> stringResource(R.string.a11y_stop_generating)
                                    hasText -> stringResource(R.string.a11y_send_button)
                                    else -> stringResource(R.string.a11y_voice_input)
                                },
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
