package com.hermes.agent.domain.model

/**
 * A single message exchanged with an LLM provider.
 *
 * Mirrors the OpenAI chat-completions message format so the same struct
 * can be sent to either a local or cloud backend without translation.
 *
 * For tool-call round-trips, set [role] = "tool" and [toolCallId] to
 * the id of the originating tool call. The LLM stitches the result
 * back into its context window and continues generating.
 */
data class LlmMessage(
    val role: String,
    val content: String,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCall>? = null,
)

/**
 * A complete (non-streaming) LLM response.
 */
data class LlmResponse(
    val content: String,
    val tokensUsed: Int,
    val model: String,
    val finishReason: String = "stop",
)

/**
 * Streaming chunk emitted by [com.hermes.agent.data.llm.LlmProvider.stream].
 */
sealed class LlmStreamChunk {
    /** Partial token of the assistant reply. */
    data class Delta(val text: String) : LlmStreamChunk()

    /** The LLM wants to invoke a tool. The orchestrator should execute it
     *  and continue the conversation with a `role=tool` reply. */
    data class ToolCallDelta(val toolCall: ToolCall) : LlmStreamChunk()

    /** Stream finished normally. */
    object Done : LlmStreamChunk()

    /** Stream finished with an error. */
    data class Error(val message: String, val cause: Throwable? = null) : LlmStreamChunk()
}
