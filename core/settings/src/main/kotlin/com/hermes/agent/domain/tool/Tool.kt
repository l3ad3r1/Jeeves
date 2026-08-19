package com.hermes.agent.domain.tool

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Type of a tool parameter. Mirrors the JSON Schema primitive types so a
 * tool definition can be serialized into an OpenAI-compatible
 * `tools` array verbatim.
 */
@Serializable
enum class ToolParameterType(val jsonSchemaType: String) {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    OBJECT("object"),
}

typealias ParameterType = ToolParameterType

/**
 * One declared parameter of a [ToolDescriptor].
 */
@Serializable
data class ToolParameter(
    val name: String,
    val type: ToolParameterType,
    val description: String,
    val required: Boolean = true,
    val enumValues: List<String>? = null,
)

/**
 * Static descriptor advertising a tool's capabilities to the LLM.
 */
@Serializable
data class ToolDescriptor(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val category: String = "general",
    val requiresConfirmation: Boolean = false,
    val maxResultSizeChars: Int = 8192,
    val requiresEnv: List<String> = emptyList(),
    val capabilities: Set<String> = emptySet(),
)

/**
 * Result returned by a [Tool] invocation.
 */
@Serializable
data class ToolResult(
    val success: Boolean,
    val output: String = "",
    val errorMessage: String? = null,
    val executionMs: Long = 0L,
) {
    companion object {
        fun ok(output: String, executionMs: Long = 0L) =
            ToolResult(success = true, output = output, executionMs = executionMs)

        fun error(message: String, executionMs: Long = 0L) =
            ToolResult(success = false, output = "", errorMessage = message, executionMs = executionMs)
    }
}

/**
 * Contract every Hermes tool must satisfy.
 */
interface Tool {
    val descriptor: ToolDescriptor
    suspend fun execute(arguments: Map<String, JsonElement>): ToolResult
}
