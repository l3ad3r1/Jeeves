package com.hermes.agent.domain.agent

import com.hermes.agent.domain.tool.Tool

data class BackupContribution(
    val id: String,
    val description: String = "",
)

data class NavEntry(
    val route: String,
    val label: String,
)

/**
 * Contract for modular agent features (e.g. Jotter notes, Butler alarms).
 *
 * Contributed via Dagger multibinding (`@Binds @IntoSet AgentFeature`) so an app
 * gets exactly the features on its classpath without hardcoded bridges.
 */
interface AgentFeature {
    val id: String
    fun tools(): List<Tool> = emptyList()              // contributed to the registry
    fun promptFragment(): String? = null               // appended to the agent prompt
    fun backupContributions(): List<BackupContribution> = emptyList()
    fun entries(): List<NavEntry> = emptyList()        // screens, if any
}
