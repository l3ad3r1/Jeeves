package com.l3ad3r1.octojotter.tools

import com.hermes.agent.domain.tool.ParameterType
import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolDescriptor
import com.hermes.agent.domain.tool.ToolParameter
import com.hermes.agent.domain.tool.ToolResult
import com.l3ad3r1.octojotter.data.local.NoteEntity
import com.l3ad3r1.octojotter.data.repository.NoteRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Write a Markdown note into Octo Jotter (`:feature:jotter`).
 */
@Singleton
class CreateNoteTool @Inject constructor(
    private val noteRepository: NoteRepository,
) : Tool {

    override val descriptor = ToolDescriptor(
        name = "create_note",
        description = "Create a Markdown note in the user's Octo Jotter notebook. Use this " +
            "when the user asks you to write something down, draft a document, or take a " +
            "note they will read later. Do NOT use this to remember facts about the user — " +
            "use the 'notes' tool for that.",
        parameters = listOf(
            ToolParameter(
                name = "title",
                type = ParameterType.STRING,
                description = "Short title for the note.",
            ),
            ToolParameter(
                name = "content",
                type = ParameterType.STRING,
                description = "Body of the note, in Markdown.",
            ),
        ),
        category = "productivity",
        capabilities = setOf("documents"),
        requiresConfirmation = false,
    )

    override suspend fun execute(arguments: Map<String, JsonElement>): ToolResult {
        val title = arguments["title"]?.extractString()?.takeIf { it.isNotBlank() }
            ?: return ToolResult.error("missing required parameter: title")
        val content = arguments["content"]?.extractString()
            ?: return ToolResult.error("missing required parameter: content")

        return try {
            val id = noteRepository.insertNote(NoteEntity(title = title, content = content))
            ToolResult.ok("note created in Octo Jotter (id=$id, title=\"$title\")")
        } catch (e: Exception) {
            ToolResult.error("could not create note: ${e.message}")
        }
    }

    private fun JsonElement.extractString(): String? =
        (this as? JsonPrimitive)?.contentOrNull
}
