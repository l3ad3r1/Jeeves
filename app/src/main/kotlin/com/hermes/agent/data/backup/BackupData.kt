package com.hermes.agent.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val schemaVersion: Int = 4,
    val exportedAt: Long = System.currentTimeMillis(),
    val memories: List<MemoryBackup> = emptyList(),
    val skills: List<SkillBackup> = emptyList(),
    // schemaVersion 2+ — null/empty when restoring an older (v1) backup.
    val settings: SettingsBackup? = null,
    val crons: List<CronBackup> = emptyList(),
    val notes: List<NoteBackup> = emptyList(),
    val alarms: List<AlarmBackup> = emptyList(),
)

@Serializable
data class MemoryBackup(
    val content: String,
    val createdAt: Long,
)

@Serializable
data class SkillBackup(
    val name: String,
    val description: String,
    val content: String,
    val category: String,
    val tags: List<String>,
    val version: String,
    val isBuiltIn: Boolean,
)

/**
 * Cloud LLM + related app settings.
 *
 * Deliberately excludes the GitHub PAT and Gist ID (those are the restore
 * credentials themselves — restoring them would be circular) and device-local
 * secrets that don't transfer between installs (API-server key, SSH config).
 */
@Serializable
data class SettingsBackup(
    val cloudEnabled: Boolean = false,
    val cloudApiKey: String = "",
    val cloudBaseUrl: String = "",
    val cloudModel: String = "",
    val reasoningEffort: String = "",
    val auxModel: String = "",
    val auxBaseUrl: String = "",
    val auxApiKey: String = "",
    val appTheme: String = "",
)

@Serializable
data class CronBackup(
    val id: String,
    val label: String,
    val prompt: String,
    val cronExpression: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class NoteBackup(
    val title: String,
    val content: String,
    val gistId: String? = null,
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val folder: String? = null,
    val locked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class AlarmBackup(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val days: Set<Int>,
)
