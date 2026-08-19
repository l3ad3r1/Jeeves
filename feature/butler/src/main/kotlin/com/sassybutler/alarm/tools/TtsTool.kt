package com.sassybutler.alarm.tools

import android.content.Context
import com.hermes.agent.domain.tool.ParameterType
import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolDescriptor
import com.hermes.agent.domain.tool.ToolParameter
import com.hermes.agent.domain.tool.ToolResult
import com.sassybutler.alarm.ButlerSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val butlerSpeech: ButlerSpeech,
) : Tool {

    override val descriptor = ToolDescriptor(
        name = "speak",
        description = "Speak text aloud through the device speaker using on-device text-to-speech. " +
            "Use this when the user asks you to say, read out, or announce something, or when a " +
            "spoken response is more useful than text (hands-free / accessibility). action='speak' " +
            "(default) reads the `text`; action='stop' halts any current speech.",
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = ParameterType.STRING,
                description = "speak (default) or stop.",
                required = false,
                enumValues = listOf("speak", "stop"),
            ),
            ToolParameter(
                name = "text",
                type = ParameterType.STRING,
                description = "The text to speak. Required for action='speak'.",
                required = false,
            ),
            ToolParameter(
                name = "voice",
                type = ParameterType.STRING,
                description = "Which engine to speak with. 'butler' (default) uses the natural " +
                    "on-device Kokoro voice; 'system' uses the platform text-to-speech engine.",
                required = false,
                enumValues = listOf("butler", "system"),
            ),
        ),
        category = "communication",
        capabilities = setOf("voice"),
    )

    override suspend fun execute(arguments: Map<String, JsonElement>): ToolResult {
        val action = arguments["action"]?.extractString()?.trim()?.lowercase() ?: "speak"

        if (action == "stop") {
            butlerSpeech.stop()
            return ToolResult.ok("Stopped speech.")
        }

        val text = arguments["text"]?.extractString()?.trim()
        if (text.isNullOrEmpty()) {
            return ToolResult.error("missing required parameter: text")
        }

        return when (butlerSpeech.speak(text)) {
            ButlerSpeech.SpeakResult.SPOKEN ->
                ToolResult.ok("Spoke aloud in Butler's voice: \"$text\"")
            ButlerSpeech.SpeakResult.STOPPED ->
                ToolResult.ok("Speech was stopped before completion.")
            ButlerSpeech.SpeakResult.UNAVAILABLE ->
                ToolResult.error("text-to-speech engine unavailable on this device")
        }
    }

    private fun JsonElement.extractString(): String? =
        (this as? JsonPrimitive)?.contentOrNull
}
