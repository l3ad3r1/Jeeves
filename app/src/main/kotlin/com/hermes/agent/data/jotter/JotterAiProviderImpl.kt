package com.hermes.agent.data.jotter

import com.hermes.agent.data.llm.CloudLlmProvider
import com.hermes.agent.data.llm.LlmMessage
import com.hermes.agent.data.llm.LlmStreamChunk
import com.hermes.agent.data.voice.VoiceOutputManager
import com.l3ad3r1.octojotter.domain.JotterAiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JotterAiProviderImpl @Inject constructor(
    private val cloudLlmProvider: CloudLlmProvider,
    private val voiceOutputManager: VoiceOutputManager
) : JotterAiProvider {

    override fun generateSummary(noteContent: String): Flow<String> {
        val messages = listOf(
            LlmMessage(role = "system", content = "You are a highly analytical AI assistant. Create a comprehensive markdown summary of the following note. Include key takeaways and action items."),
            LlmMessage(role = "user", content = noteContent)
        )
        return cloudLlmProvider.stream(messages).mapNotNull { (it as? LlmStreamChunk.Delta)?.text }
    }

    override fun generateFlashcards(noteContent: String): Flow<String> {
        val messages = listOf(
            LlmMessage(role = "system", content = "You are a study guide generator. Create a series of Q&A flashcards based on the provided note. Format as Markdown lists."),
            LlmMessage(role = "user", content = noteContent)
        )
        return cloudLlmProvider.stream(messages).mapNotNull { (it as? LlmStreamChunk.Delta)?.text }
    }

    override fun generateAudioOverview(noteContent: String): Flow<String> {
        return flow {
            emit("Generating audio overview...")
            val messages = listOf(
                LlmMessage(role = "system", content = "Summarize this note into a highly conversational, engaging podcast script for a single host. Keep it under 100 words so it can be quickly read aloud."),
                LlmMessage(role = "user", content = noteContent)
            )
            val response = cloudLlmProvider.complete(messages)
            val script = response.content.ifBlank { "Unable to generate audio script." }
            
            emit("Playing audio overview...")
            if (!voiceOutputManager.isAvailable()) {
                voiceOutputManager.initialize()
                // basic wait, ideally we listen for ready state
                kotlinx.coroutines.delay(1000)
            }
            voiceOutputManager.speak(script).collect {
                // Ignore events for simple flow
            }
            emit("Finished audio overview.")
        }
    }

    override fun chatWithNote(noteContent: String, userMessage: String): Flow<String> {
        val messages = listOf(
            LlmMessage(role = "system", content = "You are a helpful AI assistant answering questions STRICTLY based on the provided note context.\n\nContext:\n$noteContent"),
            LlmMessage(role = "user", content = userMessage)
        )
        return cloudLlmProvider.stream(messages).mapNotNull { (it as? LlmStreamChunk.Delta)?.text }
    }
}
