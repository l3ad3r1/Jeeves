package com.hermes.agent.data.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalLlmProvider @Inject constructor(
    private val localLlmManager: LocalLlmManager,
) : LlmProvider {
    override val name: String = "Llama 3.2 1B (On-Device)"
    override val isOnDevice: Boolean = true
    override val model: String = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"

    override suspend fun complete(messages: List<LlmMessage>): LlmResponse {
        // Collect the entire stream into a single string
        var fullResponse = ""
        stream(messages).collect { chunk ->
            if (chunk is LlmStreamChunk.Delta) {
                fullResponse += chunk.text
            }
        }
        return LlmResponse(
            content = fullResponse,
            tokensUsed = 0, // Not provided by local model yet
            model = model,
            finishReason = "stop"
        )
    }

    override fun stream(messages: List<LlmMessage>): Flow<LlmStreamChunk> {
        val prompt = formatPrompt(messages)
        return localLlmManager.generateResponse(prompt).map { LlmStreamChunk.Delta(it) }
    }

    override suspend fun isAvailable(): Boolean {
        return localLlmManager.isModelDownloaded()
    }

    private fun formatPrompt(messages: List<LlmMessage>): String {
        val system = messages.firstOrNull { it.role == "system" }?.content.orEmpty()
        val user = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        
        // Use the Llama 3 format that was built in the previous session
        val sb = java.lang.StringBuilder()
        sb.append("<|begin_of_text|>")
        if (system.isNotEmpty()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n")
            sb.append(system)
            sb.append("<|eot_id|>")
        }
        // Add previous messages (simplified for now, just sending the last user message)
        sb.append("<|start_header_id|>user<|end_header_id|>\n\n")
        sb.append(user)
        sb.append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }
}
