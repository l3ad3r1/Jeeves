package com.hermes.agent.domain.agent

import android.app.Application
import android.content.Context
import com.hermes.agent.data.backup.AlarmBackup
import com.hermes.agent.data.backup.NoteBackup
import com.hermes.agent.domain.tool.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class BackupContribution(
    val id: String,
    val description: String = "",
)

data class NavEntry(
    val route: String,
    val label: String,
)

data class RagDocument(
    val id: String,
    val title: String,
    val sourceUri: String,
    val mimeType: String = "text/markdown",
    val content: String,
    val createdAt: Long,
)

/**
 * Contract for modular agent features (e.g. Jotter notes, Butler alarms).
 *
 * Contributed via Dagger multibinding (`@Binds @IntoSet AgentFeature`) so an app
 * gets exactly the features on its classpath without hardcoded bridges.
 */
interface AgentFeature {
    val id: String
    val name: String get() = id
    fun tools(): List<Tool> = emptyList()              // contributed to the registry
    fun promptFragment(): String? = null               // appended to the agent prompt
    fun backupContributions(): List<BackupContribution> = emptyList()
    fun entries(): List<NavEntry> = emptyList()        // screens, if any

    fun habitInsight(context: Context): String? = null
    fun composeBriefingContext(context: Context): String? = null

    fun exportNotes(): List<NoteBackup> = emptyList()
    fun importNotes(notes: List<NoteBackup>): Int = 0
    fun exportAlarms(context: Context): List<AlarmBackup> = emptyList()
    fun importAlarms(context: Context, alarms: List<AlarmBackup>): Int = 0

    fun observeRagDocuments(): Flow<List<RagDocument>> = emptyFlow()

    fun onAppCreate(app: Application, scope: CoroutineScope) {}
}
