package com.hermes.agent.data.butler

import com.hermes.agent.data.llm.CloudLlmProvider
import com.sassybutler.alarm.di.ButlerAiProvider
import javax.inject.Inject

class ButlerAiProviderImpl @Inject constructor(
    private val cloudLlmProvider: CloudLlmProvider
) : ButlerAiProvider {

    override suspend fun generateMorningGreeting(
        weatherContext: String,
        timeContext: String,
        honorific: String,
        sassLevel: Int
    ): String? {
        val sassInstruction = when {
            sassLevel < 20 -> "Be extremely polite, patient, and saintly."
            sassLevel < 40 -> "Be polite but slightly reserved."
            sassLevel < 60 -> "Be mildly condescending about them waking up."
            sassLevel < 80 -> "Be frightfully sarcastic about the morning and their life choices."
            else -> "Be insufferably superior, acting as if waking them up is beneath your PhD."
        }

        val prompt = """
            You are Jeeves, a highly intelligent and articulate personal butler. 
            Write a very short morning wake-up greeting for $honorific.
            
            Current Time: $timeContext
            Weather: $weatherContext
            
            Instruction: $sassInstruction
            
            Requirements:
            - The greeting MUST be under 30 words.
            - It will be spoken out loud by a text-to-speech engine, so DO NOT use complex formatting, emojis, or unpronounceable words.
            - Start directly with the greeting, no pleasantries or <thought> blocks.
        """.trimIndent()

        val messages = listOf(
            com.hermes.agent.data.llm.LlmMessage(role = "system", content = prompt)
        )
        val response = cloudLlmProvider.complete(messages)
        return response.content.replace(Regex("[^a-zA-Z0-9 '.,?!-]"), "").trim()
    }
}
