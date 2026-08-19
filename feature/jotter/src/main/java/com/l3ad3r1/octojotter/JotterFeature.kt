package com.l3ad3r1.octojotter

import android.app.Application
import android.content.Context
import com.hermes.agent.data.backup.NoteBackup
import com.hermes.agent.domain.agent.AgentFeature
import com.hermes.agent.domain.agent.BackupContribution
import com.hermes.agent.domain.agent.NavEntry
import com.hermes.agent.domain.agent.RagDocument
import com.hermes.agent.domain.tool.Tool
import com.l3ad3r1.octojotter.data.local.ThemePreferences
import com.l3ad3r1.octojotter.data.local.toBackupOrNull
import com.l3ad3r1.octojotter.data.local.toRestoredEntity
import com.l3ad3r1.octojotter.data.repository.NoteRepository
import com.l3ad3r1.octojotter.tools.CreateNoteTool
import com.l3ad3r1.octojotter.tools.SearchNotesTool
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JotterFeature @Inject constructor(
    private val createNoteTool: CreateNoteTool,
    private val searchNotesTool: SearchNotesTool,
    private val noteRepository: NoteRepository,
) : AgentFeature {
    override val id: String = "jotter"
    override val name: String = "Octo Jotter"

    override fun tools(): List<Tool> = listOf(createNoteTool, searchNotesTool)

    override fun promptFragment(): String =
        "You can manage notes using create_note and search_notes."

    override fun backupContributions(): List<BackupContribution> = listOf(
        BackupContribution("jotter_notes", "Notes and notebook entries")
    )

    override fun entries(): List<NavEntry> = listOf(
        NavEntry("jotter_notes", "Notes")
    )

    override fun habitInsight(context: Context): String? {
        return try {
            val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val recentNotes = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                noteRepository.getPromptSafeRecentNotes(since)
            }
            if (recentNotes.isEmpty()) return null

            val tagCounts = recentNotes.flatMap { it.tags }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(3)

            if (tagCounts.isEmpty()) return null
            val formattedTags = tagCounts.joinToString(", ") { "${it.key} (${it.value} notes)" }
            "Over the past week, the user has been writing notes about $formattedTags."
        } catch (_: Exception) {
            null
        }
    }

    override fun exportNotes(): List<NoteBackup> {
        return try {
            val notes = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                noteRepository.allNotes.first()
            }
            notes.mapNotNull { it.toBackupOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun importNotes(notes: List<NoteBackup>): Int {
        var imported = 0
        try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val existing = noteRepository.allNotes.first().toMutableList()
                for (n in notes) {
                    val alreadyPresent = existing.any {
                        (n.gistId != null && it.gistId == n.gistId) ||
                            (n.repository != null && n.path != null &&
                                it.repository == n.repository && it.path == n.path) ||
                            (n.gistId == null && n.repository == null &&
                                it.gistId == null && it.repository == null &&
                                it.title == n.title && it.content == n.content &&
                                it.deletedAt == n.deletedAt)
                    }
                    if (alreadyPresent) continue
                    val entity = n.toRestoredEntity()
                    runCatching {
                        noteRepository.insertNote(entity)
                    }.onSuccess {
                        imported++
                        existing += entity
                    }
                }
            }
        } catch (_: Exception) {
        }
        return imported
    }

    override fun observeRagDocuments(): Flow<List<RagDocument>> =
        noteRepository.allNotes.map { notes ->
            notes.filter { !it.locked && !it.encrypted }.map { note ->
                RagDocument(
                    id = "note_${note.id}",
                    title = note.title,
                    sourceUri = "note://${note.id}",
                    mimeType = "text/markdown",
                    content = note.content,
                    createdAt = note.lastModifiedLocally,
                )
            }
        }

    override fun onAppCreate(app: Application, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            runCatching { ThemePreferences(app).migrateLegacyTheme() }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class JotterFeatureModule {
    @Binds
    @IntoSet
    abstract fun bindJotterFeature(feature: JotterFeature): AgentFeature
}
